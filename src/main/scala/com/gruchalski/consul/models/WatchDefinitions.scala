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

import scala.util.Try

sealed trait WatchDefinition

final case class WatchDefinitionKey(val key: String, val handler: String) extends WatchDefinition
final case class WatchDefinitionKeyprefix(val prefix: String, val handler: String) extends WatchDefinition
final case class WatchDefinitionServices(val handler: String) extends WatchDefinition
final case class WatchDefinitionNodes(val handler: String) extends WatchDefinition
final case class WatchDefinitionService(val service: String, val handler: String) extends WatchDefinition
final case class WatchDefinitionChecks(val handler: String) extends WatchDefinition
final case class WatchDefinitionEvent(val name: String, val handler: String) extends WatchDefinition

object WatchDefinitionUtils {

  def watchDefinitionReads[E <: WatchDefinition]: Reads[WatchDefinition] = new Reads[WatchDefinition] {
    def reads(json: JsValue): JsResult[WatchDefinition] = {
      try {
        val obj = json.asInstanceOf[JsObject]

        (obj \ "type").asOpt[String] match {
          case Some(v) if v == "key" =>
            Try(JsSuccess(
              WatchDefinitionKey(
                (obj \ "key").as[String],
                (obj \ "handler").as[String]
              )
            )).getOrElse(JsError("Failed to parse watch key definition."))
          case Some(v) if v == "keyprefix" =>
            Try(JsSuccess(
              WatchDefinitionKeyprefix(
                (obj \ "prefix").as[String],
                (obj \ "handler").as[String]
              )
            )).getOrElse(JsError("Failed to parse watch keyprefix definition."))
          case Some(v) if v == "services" =>
            Try(JsSuccess(
              WatchDefinitionServices(
                (obj \ "handler").as[String]
              )
            )).getOrElse(JsError("Failed to parse watch services definition."))
          case Some(v) if v == "nodes" =>
            Try(JsSuccess(
              WatchDefinitionNodes(
                (obj \ "handler").as[String]
              )
            )).getOrElse(JsError("Failed to parse watch nodes definition."))
          case Some(v) if v == "service" =>
            Try(JsSuccess(
              WatchDefinitionService(
                (obj \ "service").as[String],
                (obj \ "handler").as[String]
              )
            )).getOrElse(JsError("Failed to parse watch service definition."))
          case Some(v) if v == "checks" =>
            Try(JsSuccess(
              WatchDefinitionChecks(
                (obj \ "handler").as[String]
              )
            )).getOrElse(JsError("Failed to parse watch checks definition."))
          case Some(v) if v == "event" =>
            Try(JsSuccess(
              WatchDefinitionEvent(
                (obj \ "name").as[String],
                (obj \ "handler").as[String]
              )
            )).getOrElse(JsError("Failed to parse watch event definition."))
          case Some(anyOther) =>
            JsError(s"Unsupported WatchDefinition.type: $anyOther. Supported types: key, keyprefix, services, nodes, service, checks, event.")
          case None =>
            JsError(s"WatchDefinition.type missing.")
        }

      } catch {
        case _: ClassCastException => JsError(s"Expected an object but value '$json' does not look like one.")
      }
    }
  }

  def watchDefinitionWrites[E <: WatchDefinition]: Writes[WatchDefinition] = new Writes[WatchDefinition] {
    def writes(v: WatchDefinition): JsValue = {
      v match {
        case WatchDefinitionKey(key, handler) =>
          JsObject(List(
            ("type", JsString("key")),
            ("key", JsString(key)),
            ("handler", JsString(handler))
          ))
        case WatchDefinitionKeyprefix(prefix, handler) =>
          JsObject(List(
            ("type", JsString("keyprefix")),
            ("prefix", JsString(prefix)),
            ("handler", JsString(handler))
          ))
        case WatchDefinitionServices(handler) =>
          JsObject(List(
            ("type", JsString("services")),
            ("handler", JsString(handler))
          ))
        case WatchDefinitionService(service, handler) =>
          JsObject(List(
            ("type", JsString("service")),
            ("service", JsString(service)),
            ("handler", JsString(handler))
          ))
        case WatchDefinitionNodes(handler) =>
          JsObject(List(
            ("type", JsString("nodes")),
            ("handler", JsString(handler))
          ))
        case WatchDefinitionChecks(handler) =>
          JsObject(List(
            ("type", JsString("checks")),
            ("handler", JsString(handler))
          ))
        case WatchDefinitionEvent(name, handler) =>
          JsObject(List(
            ("type", JsString("event")),
            ("name", JsString(name)),
            ("handler", JsString(handler))
          ))
        case anyOther =>
          throw new UnsupportedOperationException("Unsupported watch definition in Json.writes.")
      }
    }
  }

  def watchDefinitionFormat: Format[WatchDefinition] = {
    Format(WatchDefinitionUtils.watchDefinitionReads, WatchDefinitionUtils.watchDefinitionWrites)
  }

}

trait WatchDefinitionsParser {
  implicit val watchDefinitionsFormat = WatchDefinitionUtils.watchDefinitionFormat
}

case class WatchesDefinition(val watches: List[WatchDefinition])

trait WatchesDefinitionParser extends WatchDefinitionsParser {
  implicit val watchesDefinitionFormat: Format[WatchesDefinition] =
    (__ \ "watches").format[List[WatchDefinition]].inmap(WatchesDefinition.apply, unlift(WatchesDefinition.unapply))
}
