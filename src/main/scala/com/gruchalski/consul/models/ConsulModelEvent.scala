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

case class ConsulModelEvent(
  val id: String,
  val name: String,
  val nodeFilter: String,
  val serviceFilter: String,
  val tagFilter: String,
  val version: Long,
  val ltime: Long,
  val payload: Option[String] = None
)

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
  )(ConsulModelEvent.apply, unlift(ConsulModelEvent.unapply))
}
