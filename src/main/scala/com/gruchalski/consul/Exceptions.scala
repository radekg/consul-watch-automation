package com.gruchalski.consul

import org.antlr.v4.runtime.ParserRuleContext

object Exceptions {

  case class ErrorLocation(val line: Int, val column: Int)

  case class CdfParserException(val message: String, val location: ErrorLocation)
    extends Exception(message)
  case class UseOfUndefinedVariableException(val variable: String, val location: ErrorLocation)
    extends Exception(variable)
  case class UnsupportedScopeException(val variable: String, val location: ErrorLocation)
    extends Exception(variable)
  case class NoRolesException()
    extends Exception

  def errorContext(ctx: ParserRuleContext): ErrorLocation = {
    // add 1 to char position, parser indexes columns from 0:
    ErrorLocation(ctx.getStart.getLine, ctx.getStart.getCharPositionInLine+1)
  }

}
