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

case class ConsulModelService(
  val id: String,
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
  val modifyIndex: Option[Long] = None
)

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
  )(ConsulModelService.apply, unlift(ConsulModelService.unapply))
}
