package com.gruchalski.consul.models

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class AgentKeyResponse(val key: String,
                       val createIndex: Long,
                       val modifyIndex: Long,
                       val lockIndex: Long,
                       val flags: Int,
                       val value: String,
                       val session: String)

trait AgentKeyResponseParser {
  implicit val agentKeyResponseFormat: Format[AgentKeyResponse] = (
    (__ \ "Key").format[String] and
      (__ \ "CreateIndex").format[Long] and
      (__ \ "ModifyIndex").format[Long] and
      (__ \ "LockIndex").format[Long] and
      (__ \ "Flags").format[Int] and
      (__ \ "Value").format[String] and
      (__ \ "Session").format[String]
    ) (AgentKeyResponse.apply, unlift(AgentKeyResponse.unapply))
}
