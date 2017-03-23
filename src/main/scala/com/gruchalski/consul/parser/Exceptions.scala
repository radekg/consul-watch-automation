/*
 * Copyright 2017 Rad Gruchalski
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gruchalski.consul.parser

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
    ErrorLocation(ctx.getStart.getLine, ctx.getStart.getCharPositionInLine + 1)
  }

}
