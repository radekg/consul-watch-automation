package com.gruchalski.consul.models

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class ConsulModelService(val id: String,
                              val node: String,
                              val address: String,
                              val taggedAddresses: ConsulModelTaggedAddresses,
                              val serviceId: String,
                              val serviceName: String,
                              val serviceTags: List[String],
                              val serviceAddress: String,
                              val servicePort: Int,
                              val nodeMeta: Map[String, String] = Map.empty[String, String],
                              val serviceEnableTagOverride: Boolean = false,
                              val createIndex: Option[Long] = None,
                              val modifyIndex: Option[Long] = None)

trait ConsulModelServiceParser extends ConsulModelTaggedAddressesParser {
  implicit val consulModelServiceFormat: Format[ConsulModelService] = (
    (__ \ "ID").format[String] and
      (__ \ "Node").format[String] and
      (__ \ "Address").format[String] and
      (__ \ "TaggedAddresses").format[ConsulModelTaggedAddresses] and
      (__ \ "ServiceID").format[String] and
      (__ \ "ServiceName").format[String] and
      (__ \ "ServiceTags").format[List[String]] and
      (__ \ "ServiceAddress").format[String] and
      (__ \ "ServicePort").format[Int] and
      (__ \ "NodeMeta").format[Map[String, String]] and
      (__ \ "ServiceEnableTagOverride").format[Boolean] and
      (__ \ "CreateIndex").formatNullable[Long] and
      (__ \ "ModifyIndex").formatNullable[Long]
    ) (ConsulModelService.apply, unlift(ConsulModelService.unapply))
}