package com.gruchalski.consul

object Exceptions {

  case class ErrorLocation(val line: Int, val column: Int)
  case class CdfParserException(val message: String, var location: ErrorLocation)
    extends Exception(message)

}
