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

package com.gruchalski.consul.system.actors

import akka.actor.{Actor, ActorLogging}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods.POST
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.Authorization
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import com.gruchalski.consul.models._
import com.gruchalski.consul.system.actors.HttpActorUtils.Exceptions.JsonParseFailed
import com.gruchalski.consul.system.config.Configuration
import play.api.libs.json.{Json, Reads}

import scala.concurrent.Future
import scala.util.Try

object HttpActorUtils {
  sealed trait Protocol
  final case class Bind() extends Protocol

  final case class State(val f: Option[Future[Http.ServerBinding]])

  sealed abstract class HttpException(val message: String) extends Exception(message)
  object Exceptions {
    case class UnknownWatchType(override val message: String) extends HttpException(message)
    case class InvalidInput(override val message: String) extends HttpException(message)
    case class InvalidContentType(override val message: String) extends HttpException(message)
    case class AuthenticatonException(override val message: String) extends HttpException(message)
    case class JsonParseFailed(override val message: String) extends HttpException(message)
  }
}

class HttpActor(val config: Configuration) extends Actor
    with ActorLogging
    with JsonSupport {

  implicit val system = context.system
  implicit val executionContext = context.dispatcher
  implicit val materializer = ActorMaterializer()

  private val startedAt = System.currentTimeMillis()

  private val requestHandler: (HttpRequest) => Future[HttpResponse] = {

    case HttpRequest(POST, uri, headers, entity, _) =>
      Map(
        Uri.Path("/watch/nodes") -> handleEntity[List[ConsulModelNode]] _,
        Uri.Path("/watch/services") -> handleEntity[Map[String, List[String]]] _,
        Uri.Path("/watch/key") -> handleEntity[ConsulModelKv] _,
        Uri.Path("/watch/keyprefix") -> handleEntity[List[ConsulModelKv]] _,
        Uri.Path("/watch/checks") -> handleEntity[List[ConsulModelHealthCheck]] _,
        Uri.Path("/watch/event") -> handleEntity[List[ConsulModelEvent]] _,
        Uri.Path("/watch/service") -> handleEntity[List[ConsulModelServiceHealthCheck]] _
      ).getOrElse(uri.path, (e: HttpEntity, h: Seq[HttpHeader]) => {
          Future { Right(HttpActorUtils.Exceptions.UnknownWatchType("Unknown watch type.")) }
        })(entity, headers).map({
          case Left(data) =>
            // process the data here...
            HttpResponse(StatusCodes.OK)
          case Right(cause) =>
            val status = cause match {
              case _: HttpActorUtils.Exceptions.UnknownWatchType       ⇒ StatusCodes.NotFound
              case _: HttpActorUtils.Exceptions.InvalidInput           ⇒ StatusCodes.BadRequest
              case _: HttpActorUtils.Exceptions.InvalidContentType     ⇒ StatusCodes.BadRequest
              case _: HttpActorUtils.Exceptions.AuthenticatonException ⇒ StatusCodes.Unauthorized
              case _: HttpActorUtils.Exceptions.JsonParseFailed        ⇒ StatusCodes.InternalServerError
              case anyOther                                            ⇒ StatusCodes.InternalServerError
            }
            HttpResponse(status, entity = cause.getMessage)
        })

    case r: HttpRequest =>
      Future {
        r.discardEntityBytes()
        HttpResponse(404, entity = "Unknown resource.")
      }
  }

  override def aroundPreStart(): Unit = {
    self ! HttpActorUtils.Bind()
  }

  def receive = unbound(HttpActorUtils.State(None))

  def unbound(state: HttpActorUtils.State): Receive = {
    case HttpActorUtils.Bind() =>
      val bindingFuture = Http().bindAndHandleAsync(
        requestHandler,
        config.`com.gruchalski.consul.bind.host`,
        config.`com.gruchalski.consul.bind.port`
      )
      log.info("Server bound...")
      context.become(bound(state.copy(f = Some(bindingFuture))))
  }

  def bound(state: HttpActorUtils.State): Receive = {
    case _ =>
  }

  private def handleEntity[T](entity: HttpEntity, headers: Seq[HttpHeader])(implicit tjs: Reads[T]): Future[Either[T, Throwable]] = {
    headers.filter(_.is("authorization")) match {
      case Authorization(credentials) :: _ ⇒
        if (credentials.token() == config.`com.gruchalski.consul.access-token`) {
          Unmarshal(entity).to[String].flatMap { s =>
            Try {
              if (entity.getContentType() == ContentTypes.`application/json`) {
                Json.parse(s).asOpt[T] match {
                  case Some(data) =>
                    Future {
                      Left(data)
                    }
                  case None =>
                    respondWithError(HttpActorUtils.Exceptions.InvalidInput("Input could not be parsed to the format expected by the URI."))
                }
              } else {
                respondWithError(HttpActorUtils.Exceptions.InvalidContentType("Request not application/json."))
              }
            }.getOrElse(respondWithError(JsonParseFailed("Error while parsing JSON.")))
          }
        } else {
          respondWithError(HttpActorUtils.Exceptions.AuthenticatonException("Not authorized."))
        }
      case _ ⇒
        respondWithError(HttpActorUtils.Exceptions.AuthenticatonException("Missing Authorization header."))
    }
  }

  private def respondWithError[T](error: HttpActorUtils.HttpException): Future[Either[T, Throwable]] = {
    Future { Right(error) }
  }

}
