/*
 * Copyright 2017 Rad Gruchalski
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gruchalski.consul.models

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class ConsulModelKv(
  val key: String,
  val createIndex: Long,
  val modifyIndex: Long,
  val lockIndex: Long,
  val flags: Int,
  val value: String,
  val session: Option[String] = None
)

trait ConsulModelKvParser {
  implicit val consulModelKvFormat: Format[ConsulModelKv] = (
    (__ \ "Key").format[String] and
    (__ \ "CreateIndex").format[Long] and
    (__ \ "ModifyIndex").format[Long] and
    (__ \ "LockIndex").format[Long] and
    (__ \ "Flags").format[Int] and
    (__ \ "Value").format[String] and
    (__ \ "Session").formatNullable[String]
  )(ConsulModelKv.apply, unlift(ConsulModelKv.unapply))
}
