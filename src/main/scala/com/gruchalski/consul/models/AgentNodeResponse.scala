package com.gruchalski.consul.models

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class AgentNodeResponse(val node: String, val address: String)

trait AgentNodeResponseParser {
  implicit val agentNodeResponseFormat: Format[AgentNodeResponse] = (
    (__ \ "Node").format[String] and
      (__ \ "Address").format[String]
    ) (AgentNodeResponse.apply, unlift(AgentNodeResponse.unapply))
}