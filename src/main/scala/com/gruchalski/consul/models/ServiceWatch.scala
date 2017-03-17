package com.gruchalski.consul.models

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class ServiceWatchService(val id: String,
                               val service: String,
                               val tags: Option[List[String]],
                               val port: Int)



case class ServiceWatch(val node: AgentNodeResponse,
                        val service: ServiceWatchService,
                        val checks: Option[List[AgentCheckResponse]])

trait ServiceWatchServiceParser {
  implicit val serviceWatchServiceFormat: Format[ServiceWatchService] = (
    (__ \ "ID").format[String] and
      (__ \ "Service").format[String] and
      (__ \ "Tags").formatNullable[List[String]] and
      (__ \ "Port").format[Int]
    ) (ServiceWatchService.apply, unlift(ServiceWatchService.unapply))
}


trait ServiceWatchParser extends AgentNodeResponseParser with ServiceWatchServiceParser with AgentCheckResponseParser {
  implicit val serviceWatchFormat: Format[ServiceWatch] = (
    (__ \ "Node").format[AgentNodeResponse] and
      (__ \ "Service").format[ServiceWatchService] and
      (__ \ "Checks").formatNullable[List[AgentCheckResponse]]
    ) (ServiceWatch.apply, unlift(ServiceWatch.unapply))
}
