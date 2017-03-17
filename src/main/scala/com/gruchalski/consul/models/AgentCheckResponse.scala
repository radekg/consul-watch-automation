package com.gruchalski.consul.models

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class AgentCheckResponse(val node: String,
                              val checkId: String,
                              val name: String,
                              val status: String,
                              val notes: String,
                              val output: String,
                              val serviceId: String,
                              val serviceName: String)

trait AgentCheckResponseParser {
  implicit val agentCheckResponseFormat: Format[AgentCheckResponse] = (
    (__ \ "Node").format[String] and
      (__ \ "CheckID").format[String] and
      (__ \ "Name").format[String] and
      (__ \ "Status").format[String] and
      (__ \ "Notes").format[String] and
      (__ \ "Output").format[String] and
      (__ \ "ServiceID").format[String] and
      (__ \ "ServiceName").format[String]
    ) (AgentCheckResponse.apply, unlift(AgentCheckResponse.unapply))
}
