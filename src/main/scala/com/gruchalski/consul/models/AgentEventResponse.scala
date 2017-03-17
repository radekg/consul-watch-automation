package com.gruchalski.consul.models

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class AgentEventResponse(val id: String,
                              val name: String,
                              val payload: String,
                              val nodeFilter: String,
                              val serviceFilter: String,
                              val tagFilter: String,
                              val version: Long,
                              val ltime: Long)

trait AgentEventResponseParser {
  implicit val agentEventResponseFormat: Format[AgentEventResponse] = (
    (__ \ "ID").format[String] and
      (__ \ "Name").format[String] and
      (__ \ "Payload").format[String] and
      (__ \ "NodeFilter").format[String] and
      (__ \ "ServiceFilter").format[String] and
      (__ \ "TagFilter").format[String] and
      (__ \ "Version").format[Long] and
      (__ \ "LTime").format[Long]
    ) (AgentEventResponse.apply, unlift(AgentEventResponse.unapply))
}
