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

case class ConsulModelTaggedAddresses(val lan: String, val wan: String)

case class ConsulModelNode(
  val id: String,
  val node: String,
  val address: String,
  val taggedAddresses: ConsulModelTaggedAddresses,
  val meta: Map[String, String] = Map.empty[String, String],
  val createIndex: Option[Long] = None,
  val modifyIndex: Option[Long] = None
)

trait ConsulModelTaggedAddressesParser {
  implicit val consulModelTaggedAddressesFormat: Format[ConsulModelTaggedAddresses] = (
    (__ \ "lan").format[String] and
    (__ \ "wan").format[String]
  )(ConsulModelTaggedAddresses.apply, unlift(ConsulModelTaggedAddresses.unapply))
}

trait ConsulModelNodeParser extends ConsulModelTaggedAddressesParser {
  implicit val consulModelNodeFormat: Format[ConsulModelNode] = (
    (__ \ "ID").format[String] and
    (__ \ "Node").format[String] and
    (__ \ "Address").format[String] and
    (__ \ "TaggedAddresses").format[ConsulModelTaggedAddresses] and
    (__ \ "Meta").format[Map[String, String]] and
    (__ \ "CreateIndex").formatNullable[Long] and
    (__ \ "ModifyIndex").formatNullable[Long]
  )(ConsulModelNode.apply, unlift(ConsulModelNode.unapply))
}

// used by GET /v1/catalog/node/:node
case class ConsulModelNodeExtended(
  val node: ConsulModelNode,
  val services: Map[String, ConsulModelHealthCheckService]
)

trait ConsulModelNodeExtendedParser extends ConsulModelNodeParser with ConsulModelHealthCheckServiceParser {
  implicit val consulModelNodeExtendedFormat: Format[ConsulModelNodeExtended] = (
    (__ \ "Node").format[ConsulModelNode] and
    (__ \ "Services").format[Map[String, ConsulModelHealthCheckService]]
  )(ConsulModelNodeExtended.apply, unlift(ConsulModelNodeExtended.unapply))
}
