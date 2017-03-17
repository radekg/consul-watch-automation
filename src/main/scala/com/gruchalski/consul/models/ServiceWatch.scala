package com.gruchalski.consul.models

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class ServiceWatchNode(val node: String, val address: String)

case class ServiceWatchService(val id: String,
                               val service: String,
                               val tags: Option[List[String]],
                               val port: Int)

case class ServiceWatchCheck(val node: String,
                             val checkId: String,
                             val name: String,
                             val status: String,
                             val notes: String,
                             val output: String,
                             val serviceId: String,
                             val serviceName: String)

case class ServiceWatch(val node: ServiceWatchNode,
                        val service: ServiceWatchService,
                        val checks: Option[List[ServiceWatchCheck]])

trait ServiceWatchNodeParser {
  implicit val serviceWatchNodeFormat: Format[ServiceWatchNode] = (
    (__ \ "Node").format[String] and
      (__ \ "Address").format[String]
    ) (ServiceWatchNode.apply, unlift(ServiceWatchNode.unapply))
}

trait ServiceWatchServiceParser {
  implicit val serviceWatchServiceFormat: Format[ServiceWatchService] = (
    (__ \ "ID").format[String] and
      (__ \ "Service").format[String] and
      (__ \ "Tags").formatNullable[List[String]] and
      (__ \ "Port").format[Int]
    ) (ServiceWatchService.apply, unlift(ServiceWatchService.unapply))
}

trait ServiceWatchCheckParser {
  implicit val serviceWatchCheckFormat: Format[ServiceWatchCheck] = (
    (__ \ "Node").format[String] and
      (__ \ "CheckID").format[String] and
      (__ \ "Name").format[String] and
      (__ \ "Status").format[String] and
      (__ \ "Notes").format[String] and
      (__ \ "Output").format[String] and
      (__ \ "ServiceID").format[String] and
      (__ \ "ServiceName").format[String]
    ) (ServiceWatchCheck.apply, unlift(ServiceWatchCheck.unapply))
}

trait ServiceWatchParser extends ServiceWatchNodeParser with ServiceWatchServiceParser with ServiceWatchCheckParser {
  implicit val serviceWatchFormat: Format[ServiceWatch] = (
    (__ \ "Node").format[ServiceWatchNode] and
      (__ \ "Service").format[ServiceWatchService] and
      (__ \ "Checks").formatNullable[List[ServiceWatchCheck]]
    ) (ServiceWatch.apply, unlift(ServiceWatch.unapply))
}
