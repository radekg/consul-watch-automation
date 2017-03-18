package com.gruchalski.consul.models

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class ConsulModelKv(val key: String,
                         val createIndex: Long,
                         val modifyIndex: Long,
                         val lockIndex: Long,
                         val flags: Int,
                         val value: String)

trait ConsulModelKvParser {
  implicit val consulModelKvFormat: Format[ConsulModelKv] = (
    (__ \ "Key").format[String] and
      (__ \ "CreateIndex").format[Long] and
      (__ \ "ModifyIndex").format[Long] and
      (__ \ "LockIndex").format[Long] and
      (__ \ "Flags").format[Int] and
      (__ \ "Value").format[String]
    ) (ConsulModelKv.apply, unlift(ConsulModelKv.unapply))
}
