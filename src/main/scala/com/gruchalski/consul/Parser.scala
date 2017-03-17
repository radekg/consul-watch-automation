package com.gruchalski.consul

import java.io.InputStream

import com.gruchalski.consul.Exceptions._
import com.gruchalski.consul.cdf.ConsulWatchIntegrationParser._
import com.gruchalski.consul.cdf.{ConsulWatchIntegrationLexer, ConsulWatchIntegrationParser}
import org.antlr.v4.runtime.{ANTLRInputStream, CommonTokenStream, ParserRuleContext}
import org.apache.commons.lang3.StringEscapeUtils

import scala.collection.JavaConverters._
import scala.util.Try

object ParserHelpers {

  def unquote(str: String): String = {
    if (str.startsWith("\"") && str.endsWith("\"")) {
      return str.substring(1, str.length-1)
    }
    str
  }

  def expandStringLiteral(str: String): String = {
    // TODO: implement
    StringEscapeUtils.unescapeJson(unquote(str))
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
              throw UseOfUndefinedVariableException(s"$$${varScope}.${varName}.", Exceptions.errorContext(ctx))
          }
        } else {
          throw UnsupportedScopeException(s"$$${varScope}", Exceptions.errorContext(ctx))
        }
      case _ =>
        throw CdfParserException(s"Invalid scoped variable.", Exceptions.errorContext(ctx))
    }
  }

  private def unpackUnscopedVariable(ctx: UnscopedVariableContext): String = {
    val varName = ctx.id().ID().getText
    localVars.get(varName) match {
      case Some(data) => data
      case None =>
        throw UseOfUndefinedVariableException(s"local ${varName}", Exceptions.errorContext(ctx))
    }
  }

  private def unpackVariable(ctx: VariableContext): String = {
    if (ctx.getChild(0).isInstanceOf[ScopedVariableContext]) {
      unpackScopedVariable(ctx.scopedVariable())
    } else if (ctx.getChild(0).isInstanceOf[UnscopedVariableContext]) {
      unpackUnscopedVariable(ctx.unscopedVariable())
    } else {
      throw CdfParserException(s"Invalid variable declaration.", Exceptions.errorContext(ctx))
    }
  }

  def parseProg(ctx: ProgContext): ProgramTree = {
    val roles = ctx.role().asScala.map(parseRole(_)).toList
    if (roles.isEmpty) {
      throw NoRolesException()
    }
    ProgramTree(
      roles = roles,
      log = Try(Some(parseLogDirective(ctx.logDirective()))).getOrElse(None),
      consulWatchTriggers = ctx.consulServiceChange().asScala.toList.map(parseConsulServiceChange(_)).reduce(_ ++ _) )
  }

  def parseRole(ctx: RoleContext): String = {
    ctx.id().ID().getText
  }

  def parseLogDirective(ctx: LogDirectiveContext): LogDirective = {
    ctx.children.asScala.toList match {
      case _ :: location :: _ =>
        val path = location match {
          case stringValue: StringLiteralContext =>
            ParserHelpers.expandStringLiteral(stringValue.STRING_LITERAL().getText)
          case value: VariableContext =>
            unpackVariable(value)
        }
        LogDirective(path)
      case _ =>
        throw CdfParserException(s"Invalid 'log' directive. Valid directive: log path.", Exceptions.errorContext(ctx))
    }
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
        throw CdfParserException(s"Invalid 'on' clause. Valid clause: on count role { ... }.", Exceptions.errorContext(ctx))
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
        throw CdfParserException(s"Invalid 'when_role' clause. Valid clause: when_role role { ... }.", Exceptions.errorContext(ctx))
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

        val onlyIfs = rest.map { item =>
          item match {
            case oif: OnlyIfContext =>
              Some(parseOnlyIf(oif))
            case anyOther =>
              None
          }
        }

        ExecAction(path = pathValue, onlyIf = Try(onlyIfs.head).getOrElse(None))
      case _ =>
        throw CdfParserException(s"Invalid 'exec' clause. Valid clause: exec path.", Exceptions.errorContext(ctx))
    }
  }

  def parseOnlyIf(ctx: OnlyIfContext): String = {
    ctx.children.asScala.toList match {
      case _ :: path  :: rest =>
        var pathValue = path match {
          case stringValue: StringLiteralContext =>
            ParserHelpers.expandStringLiteral(stringValue.STRING_LITERAL().getText)
          case value: VariableContext =>
            unpackVariable(value)
        }
        pathValue
      case _ =>
        throw CdfParserException(s"Invalid 'only_if' clause. Valid clause: only_if path.", Exceptions.errorContext(ctx))
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
            throw CdfParserException(s"Invalid service action.", Exceptions.errorContext(ctx))
        }
      case _ =>
        throw CdfParserException(s"Invalid system action clause.", Exceptions.errorContext(ctx))
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
        throw CdfParserException(s"Invalid 'on' clause. Valid clause: on count role { ... }.", Exceptions.errorContext(ctx))
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
        throw CdfParserException(s"Invalid template clause.", Exceptions.errorContext(ctx))
    }
  }

  def parseVia(ctx: ViaContext): ExecAction = {
    ctx.children.asScala.toList match {
      case _ :: viaElem :: rest =>
        viaElem match {
          case exec: ExecContext =>
            parseExec(exec)
          case _ =>
            throw CdfParserException(s"Invalid via clause.", Exceptions.errorContext(ctx))
        }
      case _ =>
        throw CdfParserException(s"Invalid via clause.", Exceptions.errorContext(ctx))
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
        throw CdfParserException(s"Invalid pair found.", Exceptions.errorContext(ctx))
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
            Some(num.getText.toDouble)
          case int: IntegerContext =>
            Some(int.getText.toInt)
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
