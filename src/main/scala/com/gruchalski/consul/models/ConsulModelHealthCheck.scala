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

case class ConsulModelHealthCheck(
  val node: String,
  val checkId: String,
  val name: String,
  val status: String,
  val notes: String,
  val output: String,
  val serviceId: String,
  val serviceName: String,
  val createIndex: Option[Long] = None,
  val modifyIndex: Option[Long] = None
)

trait ConsulModelHealthCheckParser {
  implicit val consulModelHealthCheckFormat: Format[ConsulModelHealthCheck] = (
    (__ \ "Node").format[String] and
    (__ \ "CheckID").format[String] and
    (__ \ "Name").format[String] and
    (__ \ "Status").format[String] and
    (__ \ "Notes").format[String] and
    (__ \ "Output").format[String] and
    (__ \ "ServiceID").format[String] and
    (__ \ "ServiceName").format[String] and
    (__ \ "CreateIndex").formatNullable[Long] and
    (__ \ "ModifyIndex").formatNullable[Long]
  )(ConsulModelHealthCheck.apply, unlift(ConsulModelHealthCheck.unapply))
}

case class ConsulModelHealthCheckService(
  val id: String,
  val service: String,
  val tags: List[String],
  val address: String,
  val port: Int,
  val enableTagOverride: Boolean = false,
  val createIndex: Option[Long] = None,
  val modifyIndex: Option[Long] = None
)

trait ConsulModelHealthCheckServiceParser {
  implicit val consulModelHealthCheckServiceFormat: Format[ConsulModelHealthCheckService] = (
    (__ \ "ID").format[String] and
    (__ \ "Service").format[String] and
    (__ \ "Tags").format[List[String]] and
    (__ \ "Address").format[String] and
    (__ \ "Port").format[Int] and
    (__ \ "EnableTagOverride").format[Boolean] and
    (__ \ "CreateIndex").formatNullable[Long] and
    (__ \ "ModifyIndex").formatNullable[Long]
  )(ConsulModelHealthCheckService.apply, unlift(ConsulModelHealthCheckService.unapply))
}

case class ConsulModelServiceHealthCheck(
  val node: ConsulModelNode,
  val service: ConsulModelHealthCheckService,
  val checks: List[ConsulModelHealthCheck]
)

trait ConsulModelServiceHealthCheckParser extends ConsulModelNodeParser
    with ConsulModelHealthCheckServiceParser
    with ConsulModelHealthCheckParser {
  implicit val consulmodelServiceHealthCheckFormat: Format[ConsulModelServiceHealthCheck] = (
    (__ \ "Node").format[ConsulModelNode] and
    (__ \ "Service").format[ConsulModelHealthCheckService] and
    (__ \ "Checks").format[List[ConsulModelHealthCheck]]
  )(ConsulModelServiceHealthCheck.apply, unlift(ConsulModelServiceHealthCheck.unapply))
}
