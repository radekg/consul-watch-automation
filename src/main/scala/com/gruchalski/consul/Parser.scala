package com.gruchalski.consul

import java.io.InputStream

import com.gruchalski.consul.Exceptions.{CdfParserException, ErrorLocation}
import com.gruchalski.consul.cdf.ConsulWatchIntegrationParser._
import com.gruchalski.consul.cdf.{ConsulWatchIntegrationLexer, ConsulWatchIntegrationParser}
import org.antlr.v4.runtime.{ANTLRInputStream, CommonTokenStream, ParserRuleContext}

import scala.collection.JavaConverters._

object ParserHelpers {
  def buildParserException(message: String, ctx: ParserRuleContext): CdfParserException = {
    CdfParserException(message, errorContext(ctx))
  }

  def errorContext(ctx: ParserRuleContext): ErrorLocation = {
    // add 1 to char position, parser indexes columns from 0:
    ErrorLocation(ctx.getStart.getLine, ctx.getStart.getCharPositionInLine+1)
  }

  def expandStringLiteral(str: String): String = {
    // TODO: implement
    str
  }
}

class Parser(val localVars: Map[String, String] = Map.empty[String, String],
             val environment: Map[String, String] = Map.empty[String, String]) {

  private def unpackScopedVariable(ctx: ScopedVariableContext): String = {
    ctx.id().asScala.toList match {
      case s :: n :: _ =>
        val varScope = s.ID().getText
        val varName = n.ID().getText
        if (varScope == ConsulSupportedScopes.envScope) {
          environment.get(varName) match {
            case Some(data) =>
              data
            case None =>
              throw ParserHelpers.buildParserException(s"Use of undeclared variable $$${varScope}.${varName}.", ctx)
          }
        } else {
          throw ParserHelpers.buildParserException(s"Unsupported scope $$${varScope}.", ctx)
        }
      case _ =>
        throw ParserHelpers.buildParserException(s"Invalid scoped variable.", ctx)
    }
  }

  private def unpackUnscopedVariable(ctx: UnscopedVariableContext): String = {
    val varName = ctx.id().ID().getText
    localVars.get(varName) match {
      case Some(data) => data
      case None =>
        throw ParserHelpers.buildParserException(s"Use of undeclared variable ${varName}.", ctx)
    }
  }

  private def unpackVariable(ctx: VariableContext): String = {
    if (ctx.getChild(0).isInstanceOf[ScopedVariableContext]) {
      unpackScopedVariable(ctx.scopedVariable())
    } else if (ctx.getChild(0).isInstanceOf[UnscopedVariableContext]) {
      unpackUnscopedVariable(ctx.unscopedVariable())
    } else {
      throw ParserHelpers.buildParserException(s"Invalid variable declaration.", ctx)
    }
  }

  def parseProg(ctx: ProgContext): ProgramTree = {
    ProgramTree(
      roles = ctx.role().asScala.map(parseRole(_)).toList,
      consulWatchTriggers = ctx.consulServiceChange().asScala.toList.map(parseConsulServiceChange(_)).reduce(_ ++ _)
    )
  }

  def parseRole(ctx: RoleContext): String = {
    ctx.id().ID().getText
  }

  def parseConsulServiceChange(ctx: ConsulServiceChangeContext): Map[Tuple2[Int, String], ConsulServiceWatch] = {
    ctx.onDef().asScala.map { input =>
      val parsed = parseOnDef(input)
      Map((parsed.count, parsed.service) -> parsed)
    }.reduce(_ ++ _)
  }

  def parseOnDef(ctx: OnDefContext): ConsulServiceWatch = {
    ctx.children.asScala.toList match {
      case _ :: count :: role :: _ =>
        val countVal = count match {
          case any: LiteralStarContext =>
            Integer.MIN_VALUE
          case intCount: IntegerContext =>
            intCount.INT().getText.toInt
          case value: VariableContext =>
            unpackVariable(value).toInt
        }
        val roleVal = role match {
          case stringValue: StringLiteralContext =>
            ParserHelpers.expandStringLiteral(stringValue.STRING_LITERAL().getText)
          case value: VariableContext =>
            unpackVariable(value)
          case id: IdContext =>
            id.ID().getText
        }
        ConsulServiceWatch(countVal, roleVal, scopes = ctx.whenRoleDef().asScala.map { whenDef =>
          val parsed = parseWhenRole(whenDef)
          parsed.role -> parsed
        }.toMap)
      case _ =>
        throw ParserHelpers.buildParserException(s"Invalid 'on' clause. Valid clause: on count role { ... }.", ctx)
    }
  }

  def parseWhenRole(ctx: WhenRoleDefContext): ConsulServiceWatchRestriction = {
    val result = ctx.children.asScala.toList match {
      case _ :: role :: rest =>

        var roleVal = role match {
          case any: LiteralStarContext =>
            None
          case id: IdContext =>
            Some(id.ID().getText)
          case stringValue: StringLiteralContext =>
            Some(ParserHelpers.expandStringLiteral(stringValue.STRING_LITERAL().getText))
          case value: VariableContext =>
            Some(unpackVariable(value))
        }

        ConsulServiceWatchRestriction(role = roleVal, actions = rest.flatMap { input =>
          input match {
            case exec: ExecContext =>
              Some(parseExec(exec))
            case template: TemplateContext =>
              Some(parseTemplate(template))
            case systemService: SystemServiceContext =>
              Some(parseSystemService(systemService))
            case consulServiceRegister: ConsulServiceRegisterContext =>
              Some(parseConsulServiceRegister(consulServiceRegister))
            case anyOther =>
              // TODO: println(s"Skipping $anyOther")
              None
          }
        })

      case _ =>
        throw ParserHelpers.buildParserException(s"Invalid 'when_role' clause. Valid clause: when_role role { ... }.", ctx)
    }
    result
  }

  def parseExec(ctx: ExecContext): ExecAction = {
    ctx.children.asScala.toList match {
      case _ :: path  :: rest =>
        var pathValue = path match {
          case stringValue: StringLiteralContext =>
            ParserHelpers.expandStringLiteral(stringValue.STRING_LITERAL().getText)
          case value: VariableContext =>
            unpackVariable(value)
        }
        ExecAction(path = pathValue)
      case _ =>
        throw ParserHelpers.buildParserException(s"Invalid 'exec' clause. Valid clause: exec path.", ctx)
    }
  }

  def parseSystemService(ctx: SystemServiceContext): SystemServiceAction = {
    ctx.children.asScala.toList match {
      case action :: service  :: _ =>
        var serviceValue = service match {
          case stringValue: StringLiteralContext =>
            ParserHelpers.expandStringLiteral(stringValue.STRING_LITERAL().getText)
          case value: VariableContext =>
            unpackVariable(value)
          case id: IdContext =>
            id.ID().getText
        }
        try {
          val actionValue = action.asInstanceOf[SystemServiceActionContext].getText
          SystemServiceAction(SystemServiceActions(actionValue), serviceValue)
        } catch {
          case e: UnsupportedSystemActionException =>
            throw ParserHelpers.buildParserException(s"Invalid service action.", ctx)
        }
      case _ =>
        throw ParserHelpers.buildParserException(s"Invalid system action clause.", ctx)
    }
  }

  def parseConsulServiceRegister(ctx: ConsulServiceRegisterContext): CreateConsulServiceAction = {
    ctx.children.asScala.toList match {
      case _ :: service :: rest =>
        val serviceValue = service match {
          case id: IdContext =>
            id.ID().getText
          case stringValue: StringLiteralContext =>
            ParserHelpers.expandStringLiteral(stringValue.STRING_LITERAL().getText)
          case value: VariableContext =>
            unpackVariable(value)
        }
        val bodies = rest.flatMap { item =>
          item match {
            case body: ObjContext =>
              Some(parseObject(body))
            case anyOther =>
              None
          }
        }
        CreateConsulServiceAction(serviceValue, bodies.headOption.getOrElse(Map.empty[String, Any]))
      case _ =>
        throw ParserHelpers.buildParserException(s"Invalid consul_service_register action.", ctx)
    }
  }

  def parseTemplate(ctx: TemplateContext): TemplateAction = {
    ctx.children.asScala.toList match {
      case _ :: source :: destination :: rest =>
        val sourceValue = source match {
          case stringLiteral: StringLiteralContext =>
            ParserHelpers.expandStringLiteral(stringLiteral.STRING_LITERAL().getText)
          case value: VariableContext =>
            unpackVariable(value)
        }
        val destinationValue = destination match {
          case stringLiteral: StringLiteralContext =>
            ParserHelpers.expandStringLiteral(stringLiteral.STRING_LITERAL().getText)
          case value: VariableContext =>
            unpackVariable(value)
        }

        val vias = rest.flatMap { item =>
          item match {
            case via: ViaContext =>
              Some(parseVia(via))
            case anyOther =>
              None
          }
        }
        val bodies = rest.flatMap { item =>
          item match {
            case body: ObjContext =>
              Some(parseObject(body))
            case anyOther =>
              None
          }
        }

        TemplateAction(sourceValue, destinationValue, params=bodies.headOption.getOrElse(Map.empty[String, Any]), via=vias.headOption)
      case _ =>
        throw ParserHelpers.buildParserException(s"Invalid template clause.", ctx)
    }
  }

  def parseVia(ctx: ViaContext): ExecAction = {
    ctx.children.asScala.toList match {
      case _ :: viaElem :: rest =>
        viaElem match {
          case exec: ExecContext =>
            parseExec(exec)
          case _ =>
            throw ParserHelpers.buildParserException(s"Invalid via clause.", ctx)
        }
      case _ =>
        throw ParserHelpers.buildParserException(s"Invalid via clause.", ctx)
    }
  }

  def parseObject(ctx: ObjContext): Map[String, Any] = {
    ctx.pair().asScala.flatMap(parsePair(_)).reduce(_ ++ _)
  }

  def parsePair(ctx: PairContext): Option[Map[String, Any]] = {
    ctx.children.asScala.toList match {
      case key :: _ :: value :: _ =>
        val keyName = key.getText
        parseValue(value.asInstanceOf[ValueContext]) match {
          case Some(parsed) =>
            Some(Map(keyName -> parsed))
          case None =>
            None
        }
      case _ =>
        throw ParserHelpers.buildParserException(s"Invalid pair found.", ctx)
    }
  }

  def parseArray(ctx: ArrayContext): List[Any] = {
    ctx.children.asScala.flatMap { item =>
      item match {
        case value: ValueContext =>
          Some(parseValue(value))
        case _ => None
      }
    }.toList.flatten
  }

  def parseValue(value: ValueContext): Option[Any] = {
    value.children.asScala.headOption match {
      case Some(opt) =>
        opt match {
          case str: StringLiteralContext =>
            Some(ParserHelpers.expandStringLiteral(str.STRING_LITERAL().getText))
          case v4r: VariableContext =>
            Some(unpackVariable(v4r))
          case num: NumberContext =>
            Some(num.getText)
          case arr: ArrayContext =>
            Some(parseArray(arr))
          case obj: ObjContext =>
            Some(parseObject(obj))
          case id: IdContext =>
            Some(id.getText)
          case anyOther =>
            None
        }
      case None => None
    }
  }

}

object Parser {

  type Vars = Map[String, String]

  def emptyVars(): Vars = {
    Map.empty[String, String]
  }

  def parse(is: InputStream, localVars: Vars = emptyVars(), environment: Vars = emptyVars()): Any = {
    val prog = new ConsulWatchIntegrationParser(
      new CommonTokenStream(
        new ConsulWatchIntegrationLexer(
          new ANTLRInputStream(is)))).prog()
    new Parser(localVars, environment).parseProg(prog)
  }

}
