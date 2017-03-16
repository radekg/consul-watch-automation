package com.gruchalski.consul.cdf

import java.util

import com.gruchalski.consul.Exceptions.{CdfParserException, ErrorLocation}
import com.gruchalski.consul._
import com.gruchalski.consul.cdf.ConsulWatchIntegrationParser._
import org.antlr.v4.runtime.ParserRuleContext

import scala.collection.JavaConverters._

class CdfVisitor(val localVars: Map[String, String] = Map.empty[String, String],
                 val environment: Map[String, String] = Map.empty[String, String])
  extends ConsulWatchIntegrationBaseVisitor[Any] {

  val envVars = System.getenv().asScala.toMap ++ environment

  private val _roles = new util.ArrayList[String]()
  def roles = _roles

  private val _watches = new util.HashMap[Tuple2[Int, String], ConsulWatchTrigger]()
  def watches = _watches

  private var _currentWatchTrigger: ConsulWatchTrigger = _
  private var _currentWatchTriggerScope: ConsulWatchTriggerScope = _
  private var _currentWithArguments: Option[ActionWithArguments] = None

  private def buildParserException(message: String, ctx: ParserRuleContext): CdfParserException = {
    CdfParserException(message, errorContext(ctx))
  }

  private def errorContext(ctx: ParserRuleContext): ErrorLocation = {
    // add 1 to char position, parser indexes columns from 0:
    ErrorLocation(ctx.getStart.getLine, ctx.getStart.getCharPositionInLine+1)
  }

  private def expandStringLiteral(str: String): String = {
    // TODO: implement
    str
  }

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
              throw buildParserException(s"Use of undeclared variable $$${varScope}.${varName}.", ctx)
          }
        } else {
          throw buildParserException(s"Unsupported scope $$${varScope}.", ctx)
        }
      case _ =>
        throw buildParserException(s"Invalid scoped variable.", ctx)
    }
  }

  private def unpackUnscopedVariable(ctx: UnscopedVariableContext): String = {
    val varName = ctx.id().ID().getText
    localVars.get(varName) match {
      case Some(data) => data
      case None =>
        throw buildParserException(s"Use of undeclared variable ${varName}.", ctx)
    }
  }

  private def unpackVariable(ctx: VariableContext): String = {
    if (ctx.getChild(0).isInstanceOf[ScopedVariableContext]) {
      unpackScopedVariable(ctx.scopedVariable())
    } else if (ctx.getChild(0).isInstanceOf[UnscopedVariableContext]) {
      unpackUnscopedVariable(ctx.unscopedVariable())
    } else {
      throw buildParserException(s"Invalid variable declaration.", ctx)
    }
  }

  override def visitProg(ctx: ProgContext): AnyRef = {
    if (ctx.role().isEmpty) {
      throw buildParserException("No roles defined in the service file.", ctx)
    }
    ctx.role().asScala.toList.foreach { role =>
      _roles.add(role.id().getText)
    }
    ctx.consulServiceChange().asScala.toList.foreach { csc =>
      visit(csc)
    }
    ProgramTree(roles.asScala.toList, watches.asScala.toMap)
  }

  override def visitOnDef(ctx: OnDefContext): AnyRef = {
    ctx.children.asScala.toList match {
      case _ :: count :: role :: rest =>
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
            expandStringLiteral(stringValue.STRING_LITERAL().getText)
          case value: VariableContext =>
            unpackVariable(value)
          case id: IdContext =>
            id.ID().getText
        }
        _currentWatchTrigger = new ConsulWatchTrigger(countVal, roleVal)
        rest.foreach(visit(_))
        _watches.put((countVal, roleVal), _currentWatchTrigger)
      case _ =>
        throw buildParserException(s"Invalid 'on' clause. Valid clause: on count role { ... }.", ctx)
    }

    ctx
  }

  override def visitWhenRoleDef(ctx: WhenRoleDefContext): AnyRef = {
    ctx.children.asScala.toList match {
      case _ :: role :: rest =>
        var roleVal = role match {
          case any: LiteralStarContext =>
            None
          case id: IdContext =>
            Some(id.ID().getText)
          case stringValue: StringLiteralContext =>
            Some(expandStringLiteral(stringValue.STRING_LITERAL().getText))
          case value: VariableContext =>
            Some(unpackVariable(value))
        }
        _currentWatchTriggerScope = new ConsulWatchTriggerScope(role = roleVal)
        rest.foreach(visit(_))
        _currentWatchTrigger.scopes.put(roleVal, _currentWatchTriggerScope)
      case _ =>
        throw buildParserException(s"Invalid 'when_role' clause. Valid clause: when_role role { ... }.", ctx)
    }
    ctx
  }

  override def visitExec(ctx: ExecContext): AnyRef = {
    ctx.children.asScala.toList match {
      case _ :: path  :: rest =>
        var pathValue = path match {
          case stringValue: StringLiteralContext =>
            expandStringLiteral(stringValue.STRING_LITERAL().getText)
          case value: VariableContext =>
            unpackVariable(value)
        }
        _currentWatchTriggerScope.actions.add(new ExecAction(path = pathValue))
        rest.foreach(visit(_))
      case _ =>
        throw buildParserException(s"Invalid 'exec' clause. Valid clause: exec path.", ctx)
    }
    ctx
  }

  override def visitSystemService(ctx: SystemServiceContext): AnyRef = {
    ctx.children.asScala.toList match {
      case action :: service  :: rest =>
        var serviceValue = service match {
          case stringValue: StringLiteralContext =>
            expandStringLiteral(stringValue.STRING_LITERAL().getText)
          case value: VariableContext =>
            unpackVariable(value)
          case id: IdContext =>
            id.ID().getText
        }
        try {
          val actionValue = action.asInstanceOf[SystemServiceActionContext].getText
          _currentWatchTriggerScope.actions.add(
            new SystemServiceAction(SystemServiceActions(actionValue), serviceValue))
          rest.foreach(visit(_))
        } catch {
          case e: UnsupportedSystemActionException =>
            throw buildParserException(s"Invalid service action.", ctx)
        }
        rest.foreach(visit(_))
      case _ =>
        throw new RuntimeException(s"Invalid system action clause found at ${errorContext(ctx)}.")
    }
    ctx
  }

  override def visitConsulServiceRegister(ctx: ConsulServiceRegisterContext): AnyRef = {
    ctx.children.asScala.toList match {
      case _ :: service :: rest =>
        val serviceValue = service match {
          case id: IdContext =>
            id.ID().getText
          case stringValue: StringLiteralContext =>
            expandStringLiteral(stringValue.STRING_LITERAL().getText)
          case value: VariableContext =>
            unpackVariable(value)
        }
        val currentWithArgs = new CreateConsulServiceAction(serviceValue)
        _currentWithArguments = Some(currentWithArgs)
        rest.foreach(visit(_))
        _currentWatchTriggerScope.actions.add(currentWithArgs)
      case _ =>
        throw buildParserException(s"Invalid consul_service_register action.", ctx)
    }
    ctx
  }

  override def visitTemplate(ctx: TemplateContext): AnyRef = {
    ctx.children.asScala.toList match {
      case _ :: source :: destination :: rest =>
        val sourceValue = source match {
          case stringLiteral: StringLiteralContext =>
            expandStringLiteral(stringLiteral.STRING_LITERAL().getText)
          case value: VariableContext =>
            unpackVariable(value)
        }
        val destinationValue = destination match {
          case stringLiteral: StringLiteralContext =>
            expandStringLiteral(stringLiteral.STRING_LITERAL().getText)
          case value: VariableContext =>
            unpackVariable(value)
        }
        val currentWithArgs = new TemplateAction(sourceValue, destinationValue)
        _currentWithArguments = Some(currentWithArgs)
        rest.foreach(visit(_))
        _currentWatchTriggerScope.actions.add(currentWithArgs)
      case _ =>
        throw buildParserException(s"Invalid template clause.", ctx)
    }
    ctx
  }

  override def visitVia(ctx: ViaContext): AnyRef = {
    ctx.children.asScala.toList match {
      case _ :: viaElem :: rest =>
        viaElem match {
          case exec: ExecContext =>
            val execValue = exec.children.asScala.toList match {
              case _ :: execVal :: _ =>
                execVal match {
                  case stringVal: StringLiteralContext =>
                    expandStringLiteral(stringVal.STRING_LITERAL().getText)
                  case value: VariableContext =>
                    unpackVariable(value)
                }
              case _ =>
                throw buildParserException(s"Invalid via exec clause.", ctx)
            }
            _currentWithArguments match {
              case Some(ref) =>
                if (ref.isInstanceOf[TemplateAction]) {
                  val newRef = ref.asInstanceOf[TemplateAction].copy(via = Some(execValue))
                  _currentWatchTriggerScope.actions.add(_currentWatchTriggerScope.actions.size()-1, newRef)
                  _currentWithArguments = Some(newRef)
                }
              case None =>
                // TODO: log warning here...
            }
        }
        rest.foreach(visit(_))
      case _ =>
        throw buildParserException(s"Invalid via clause.", ctx)
    }
    ctx
  }

  override def visitHashLikeParam(ctx: HashLikeParamContext): AnyRef = {
    _currentWithArguments match {
      case Some(hashLike) =>
        ctx.children.asScala.toList match {
          case id :: _ :: value :: rest =>
            val idValue = id.getText
            val valueVal = value match {
              case stringValue: StringLiteralContext =>
                expandStringLiteral(stringValue.STRING_LITERAL().getText)
              case variableValue: VariableContext =>
                unpackVariable(variableValue)
              case intValue: IntegerContext =>
                intValue.INT().getText.toInt
            }
            hashLike.arguments.put(idValue, valueVal)
            rest.foreach(visit(_))
          case _ =>
            throw buildParserException(s"Invalid key / value.", ctx)
        }
      case None =>
        // todo: log warning here...
    }
    ctx
  }

}