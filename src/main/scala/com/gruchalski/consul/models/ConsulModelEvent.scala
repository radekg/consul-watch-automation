package com.gruchalski.consul.models

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class ConsulModelEvent(val id: String,
                            val name: String,
                            val nodeFilter: String,
                            val serviceFilter: String,
                            val tagFilter: String,
                            val version: Long,
                            val ltime: Long,
                            val payload: Option[String] = None)

trait ConsulModelEventParser {
  implicit val consulModelEventFormat: Format[ConsulModelEvent] = (
    (__ \ "ID").format[String] and
      (__ \ "Name").format[String] and
      (__ \ "NodeFilter").format[String] and
      (__ \ "ServiceFilter").format[String] and
      (__ \ "TagFilter").format[String] and
      (__ \ "Version").format[Long] and
      (__ \ "LTime").format[Long] and
      (__ \ "Payload").formatNullable[String]
    ) (ConsulModelEvent.apply, unlift(ConsulModelEvent.unapply))
}
