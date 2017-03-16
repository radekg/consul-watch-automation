package com.gruchalski.consul.cdf

import java.util

import com.gruchalski.consul.{ConsulSupportedScopes, ConsulWatchTrigger}
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

  private def errorContext(ctx: ParserRuleContext): String = {
    // add 1 to char position, parser indexes columns from 0:
    s"line ${ctx.getStart.getLine}, column ${(ctx.getStart.getCharPositionInLine+1)}"
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
              throw new RuntimeException(s"Use of undeclared variable $$${varScope}.${varName} found at ${errorContext(ctx)}.")
          }
        } else {
          throw new RuntimeException(s"Unsupported scope $$${varScope} found at ${errorContext(ctx)}.")
        }
      case _ =>
        throw new RuntimeException(s"Invalid scoped variable found at ${errorContext(ctx)}.")
    }
  }

  private def unpackUnscopedVariable(ctx: UnscopedVariableContext): String = {
    val varName = ctx.id().ID().getText
    localVars.get(varName) match {
      case Some(data) => data
      case None =>
        throw new RuntimeException(s"Use of undeclared variable ${varName} found at ${errorContext(ctx)}.")
    }
  }

  private def unpackVariable(ctx: VariableContext): String = {
    if (ctx.getChild(0).isInstanceOf[ScopedVariableContext]) {
      unpackScopedVariable(ctx.scopedVariable())
    } else if (ctx.getChild(0).isInstanceOf[UnscopedVariableContext]) {
      unpackUnscopedVariable(ctx.unscopedVariable())
    } else {
      throw new RuntimeException(s"Invalid variable declaration found at ${errorContext(ctx)}.")
    }
  }

  override def visitProg(ctx: ProgContext): AnyRef = {
    if (ctx.role().isEmpty) {
      throw new RuntimeException("No roles defined in the service file.")
    }
    ctx.role().asScala.toList.foreach { role =>
      _roles.add(role.id().getText)
    }
    ctx.consulServiceChange().asScala.toList.foreach { csc =>
      visit(csc)
    }
    (roles, watches)
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
            stringValue.STRING_LITERAL().getText
          case value: VariableContext =>
            expandStringLiteral(unpackVariable(value))
        }
        val watchTrigger = new ConsulWatchTrigger(countVal, roleVal)
        rest.foreach(visit(_))
        _watches.put((countVal, roleVal), watchTrigger)
      case _ =>
        throw new RuntimeException(s"Invalid 'when' clause found at ${errorContext(ctx)}.")
    }

    ctx
  }

  override def visitWhenRoleDef(ctx: WhenRoleDefContext): AnyRef = {
    println(" ====================> when_role")
    ctx
  }

}