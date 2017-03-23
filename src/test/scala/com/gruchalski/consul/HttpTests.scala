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

package com.gruchalski.consul

import java.net.ServerSocket
import java.util.UUID

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.http.scaladsl.model.{HttpEntity, _}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import com.gruchalski.consul.models._
import com.gruchalski.consul.system.Server
import com.gruchalski.consul.utils.ConfigDefaults
import com.typesafe.config.{ConfigFactory, ConfigValueFactory}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}
import play.api.libs.json.{JsValue, Json}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.Try

case class RequestExecutor(port: Int) {

  def entityExecute(entity: RequestEntity, path: Path, method: HttpMethod, expectedStatus: StatusCode, headers: Seq[HttpHeader] = Seq.empty[HttpHeader])(implicit system: ActorSystem, materializer: ActorMaterializer): Future[Either[String, String]] =
    Source.single(HttpRequest(
      uri = Uri(path = path),
      method = method,
      entity = entity
    ).withHeaders(headers: _*)).via(Http().outgoingConnection("localhost", port).mapAsync(1) { response =>
      response.status match {
        case `expectedStatus` => Future(Left("ok"))
        case anyOther         => Future(Right(s"Unexpected status code: $anyOther. Expected the result to be $expectedStatus."))
      }
    }).runWith(Sink.head)

  def dataExecute(data: String, path: Path, method: HttpMethod, expectedStatus: StatusCode, headers: Seq[HttpHeader] = Seq.empty[HttpHeader])(implicit system: ActorSystem, materializer: ActorMaterializer): Future[Either[String, String]] =
    entityExecute(HttpEntity(ContentType(MediaTypes.`application/json`), data), path, method, expectedStatus, headers)

  def postJsonExecute(json: JsValue, path: Path, expectedStatus: StatusCode, headers: Seq[HttpHeader] = Seq.empty[HttpHeader])(implicit system: ActorSystem, materializer: ActorMaterializer): Future[Either[String, String]] =
    dataExecute(json.toString(), path, HttpMethods.POST, expectedStatus, headers)

}

class HttpTests extends WordSpec with Matchers with BeforeAndAfterAll with JsonSupport {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  private def freePortSafe(): Try[Int] = {
    Try {
      val socket = new ServerSocket(0)
      val port = socket.getLocalPort
      socket.close()
      port
    }
  }

  private val server = Server()
  private val config = ConfigDefaults(Map(
    "com.gruchalski.consul.bind.port" → freePortSafe().get,
    "com.gruchalski.consul.access-token" → "unit-test-token"
  ))

  override def beforeAll {
    server.start(Some(config))
  }

  override def afterAll {
    server.stop()
    system.terminate()
  }

  "HTTP server" must {

    "return Unauthorized" when {

      "requesting a response from a valid URI with valid data but without the token" in {
        val data = Json.toJson(
          ConsulModelKv(
            lockIndex = 0,
            key = "some-key",
            flags = 0,
            value = "c29tZS1kYXRh",
            createIndex = 28,
            modifyIndex = 28,
            session = Some("")
          )
        )
        server.maybeConfiguration().map { config =>
          Await.result(
            RequestExecutor(config.`com.gruchalski.consul.bind.port`).postJsonExecute(
              data,
              Path("/watch/key"),
              StatusCodes.Unauthorized,
              List.empty[HttpHeader]
            ), 1 second
          ) should matchPattern { case Left(_) => }
        }.orElse(fail("Expected configuration in the server."))
      }

      "requesting a response from a valid URI with valid data with an invalid token" in {
        val data = Json.toJson(
          ConsulModelKv(
            lockIndex = 0,
            key = "some-key",
            flags = 0,
            value = "c29tZS1kYXRh",
            createIndex = 28,
            modifyIndex = 28,
            session = Some("")
          )
        )
        server.maybeConfiguration().map { config =>
          Await.result(
            RequestExecutor(config.`com.gruchalski.consul.bind.port`).postJsonExecute(
              data,
              Path("/watch/key"),
              StatusCodes.Unauthorized,
              List(Authorization(OAuth2BearerToken(config.`com.gruchalski.consul.access-token` + "invalid")))
            ), 1 second
          ) should matchPattern { case Left(_) => }
        }.orElse(fail("Expected configuration in the server."))
      }

    }

    "return NotFound" when {

      "requesting a response from a non-existing URI" in {

        server.maybeConfiguration().map { config =>
          Await.result(
            RequestExecutor(config.`com.gruchalski.consul.bind.port`).dataExecute(
              "a request to a non-existing URI",
              Path("/non/existing/uri"),
              HttpMethods.GET,
              StatusCodes.NotFound,
              List(Authorization(OAuth2BearerToken(config.`com.gruchalski.consul.access-token`)))
            ), 1 second
          ) should matchPattern { case Left(_) => }
        }.orElse(fail("Expected configuration in the server."))

      }

      "requesting to process an unknown watch type" in {
        server.maybeConfiguration().map { config =>
          Await.result(
            RequestExecutor(config.`com.gruchalski.consul.bind.port`).postJsonExecute(
              Json.parse("""{"json": "data"}"""),
              Path("/watch/unknown-type"),
              StatusCodes.NotFound,
              List(Authorization(OAuth2BearerToken(config.`com.gruchalski.consul.access-token`)))
            ), 1 second
          ) should matchPattern { case Left(_) => }
        }.orElse(fail("Expected configuration in the server."))
      }

    }

    "return BadRequest" when {

      "/watch/nodes is given an invalid data" in {
        server.maybeConfiguration().map { config =>
          Await.result(
            RequestExecutor(config.`com.gruchalski.consul.bind.port`).postJsonExecute(
              Json.parse("""{"not": "valid", "for": "this", "request": []}"""),
              Path("/watch/nodes"),
              StatusCodes.BadRequest,
              List(Authorization(OAuth2BearerToken(config.`com.gruchalski.consul.access-token`)))
            ), 1 second
          ) should matchPattern { case Left(_) => }
        }.orElse(fail("Expected configuration in the server."))
      }

      "/watch/services is given an invalid data" in {
        server.maybeConfiguration().map { config =>
          Await.result(
            RequestExecutor(config.`com.gruchalski.consul.bind.port`).postJsonExecute(
              Json.parse("""{"not": "valid", "for": "this", "request": []}"""),
              Path("/watch/services"),
              StatusCodes.BadRequest,
              List(Authorization(OAuth2BearerToken(config.`com.gruchalski.consul.access-token`)))
            ), 1 second
          ) should matchPattern { case Left(_) => }
        }.orElse(fail("Expected configuration in the server."))
      }

      "/watch/service is given an invalid data" in {
        server.maybeConfiguration().map { config =>
          Await.result(
            RequestExecutor(config.`com.gruchalski.consul.bind.port`).postJsonExecute(
              Json.parse("""{"not": "valid", "for": "this", "request": []}"""),
              Path("/watch/service"),
              StatusCodes.BadRequest,
              List(Authorization(OAuth2BearerToken(config.`com.gruchalski.consul.access-token`)))
            ), 1 second
          ) should matchPattern { case Left(_) => }
        }.orElse(fail("Expected configuration in the server."))
      }

      "/watch/key is given an invalid data" in {
        server.maybeConfiguration().map { config =>
          Await.result(
            RequestExecutor(config.`com.gruchalski.consul.bind.port`).postJsonExecute(
              Json.parse("""{"not": "valid", "for": "this", "request": []}"""),
              Path("/watch/key"),
              StatusCodes.BadRequest,
              List(Authorization(OAuth2BearerToken(config.`com.gruchalski.consul.access-token`)))
            ), 1 second
          ) should matchPattern { case Left(_) => }
        }.orElse(fail("Expected configuration in the server."))
      }

      "/watch/keyprefix is given an invalid data" in {
        server.maybeConfiguration().map { config =>
          Await.result(
            RequestExecutor(config.`com.gruchalski.consul.bind.port`).postJsonExecute(
              Json.parse("""{"not": "valid", "for": "this", "request": []}"""),
              Path("/watch/keyprefix"),
              StatusCodes.BadRequest,
              List(Authorization(OAuth2BearerToken(config.`com.gruchalski.consul.access-token`)))
            ), 1 second
          ) should matchPattern { case Left(_) => }
        }.orElse(fail("Expected configuration in the server."))
      }

      "/watch/checks is given an invalid data" in {
        server.maybeConfiguration().map { config =>
          Await.result(
            RequestExecutor(config.`com.gruchalski.consul.bind.port`).postJsonExecute(
              Json.parse("""{"not": "valid", "for": "this", "request": []}"""),
              Path("/watch/checks"),
              StatusCodes.BadRequest,
              List(Authorization(OAuth2BearerToken(config.`com.gruchalski.consul.access-token`)))
            ), 1 second
          ) should matchPattern { case Left(_) => }
        }.orElse(fail("Expected configuration in the server."))
      }

      "/watch/event is given an invalid data" in {
        server.maybeConfiguration().map { config =>
          Await.result(
            RequestExecutor(config.`com.gruchalski.consul.bind.port`).postJsonExecute(
              Json.parse("""{"not": "valid", "for": "this", "request": []}"""),
              Path("/watch/event"),
              StatusCodes.BadRequest,
              List(Authorization(OAuth2BearerToken(config.`com.gruchalski.consul.access-token`)))
            ), 1 second
          ) should matchPattern { case Left(_) => }
        }.orElse(fail("Expected configuration in the server."))
      }

      "a valid watch endpoint is given a request without application/json header" in {
        server.maybeConfiguration().map { config =>
          Await.result(
            RequestExecutor(config.`com.gruchalski.consul.bind.port`).entityExecute(
              HttpEntity("no content type"),
              Path("/watch/nodes"),
              HttpMethods.POST,
              StatusCodes.BadRequest,
              List(Authorization(OAuth2BearerToken(config.`com.gruchalski.consul.access-token`)))
            ), 1 second
          ) should matchPattern { case Left(_) => }
        }.orElse(fail("Expected configuration in the server."))
      }

    }

    "return Ok" when {

      "given a valid /watch/nodes request" in {
        val data = Json.toJson(
          List(
            ConsulModelNode(
              id = UUID.randomUUID().toString(),
              node = "unit-test-node",
              address = "127.0.0.1",
              taggedAddresses = ConsulModelTaggedAddresses(
                lan = "127.0.0.1",
                wan = "127.0.0.1"
              )
            )
          )
        )
        server.maybeConfiguration().map { config =>
          Await.result(
            RequestExecutor(config.`com.gruchalski.consul.bind.port`).postJsonExecute(
              data,
              Path("/watch/nodes"),
              StatusCodes.OK,
              List(Authorization(OAuth2BearerToken(config.`com.gruchalski.consul.access-token`)))
            ), 1 second
          ) should matchPattern { case Left(_) => }
        }.orElse(fail("Expected configuration in the server."))
      }

      "given a valid /watch/services request" in {
        val data = Json.toJson(Map(
          "consul" -> List.empty[String],
          "support-service" -> List.empty[String]
        ))
        server.maybeConfiguration().map { config =>
          Await.result(
            RequestExecutor(config.`com.gruchalski.consul.bind.port`).postJsonExecute(
              data,
              Path("/watch/services"),
              StatusCodes.OK,
              List(Authorization(OAuth2BearerToken(config.`com.gruchalski.consul.access-token`)))
            ), 1 second
          ) should matchPattern { case Left(_) => }
        }.orElse(fail("Expected configuration in the server."))
      }

      "given a valid /watch/service request" in {
        val data = Json.toJson(
          List(
            ConsulModelServiceHealthCheck(
              node = ConsulModelNode(
                id = "622d2470-c34d-31f9-a62b-6aa9a3c6a3cd",
                node = "support-node",
                address = "127.0.0.1",
                taggedAddresses = ConsulModelTaggedAddresses(
                  lan = "127.0.0.1",
                  wan = "127.0.0.1"
                )
              ),
              service = ConsulModelHealthCheckService(
                id = "support-service.1",
                service = "support-service",
                tags = List.empty[String],
                address = "127.0.0.1",
                port = 0
              ),
              checks = List(ConsulModelHealthCheck(
                node = "support-node",
                checkId = "serfHealth",
                name = "Serf Health Status",
                status = "passing",
                notes = "",
                output = "Agent alive and reachable",
                serviceId = "",
                serviceName = ""
              ))
            )
          )
        )
        server.maybeConfiguration().map { config =>
          Await.result(
            RequestExecutor(config.`com.gruchalski.consul.bind.port`).postJsonExecute(
              data,
              Path("/watch/service"),
              StatusCodes.OK,
              List(Authorization(OAuth2BearerToken(config.`com.gruchalski.consul.access-token`)))
            ), 1 second
          ) should matchPattern { case Left(_) => }
        }.orElse(fail("Expected configuration in the server."))
      }

      "given a valid /watch/key request" in {
        val data = Json.toJson(
          ConsulModelKv(
            lockIndex = 0,
            key = "some-key",
            flags = 0,
            value = "c29tZS1kYXRh",
            createIndex = 28,
            modifyIndex = 28,
            session = Some("")
          )
        )
        server.maybeConfiguration().map { config =>
          Await.result(
            RequestExecutor(config.`com.gruchalski.consul.bind.port`).postJsonExecute(
              data,
              Path("/watch/key"),
              StatusCodes.OK,
              List(Authorization(OAuth2BearerToken(config.`com.gruchalski.consul.access-token`)))
            ), 1 second
          ) should matchPattern { case Left(_) => }
        }.orElse(fail("Expected configuration in the server."))
      }

      "given a valid /watch/keyprefix request" in {
        val data = Json.toJson(
          List(
            ConsulModelKv(
              lockIndex = 0,
              key = "some-key",
              flags = 0,
              value = "c29tZS1kYXRh",
              createIndex = 28,
              modifyIndex = 28,
              session = Some("")
            )
          )
        )
        server.maybeConfiguration().map { config =>
          Await.result(
            RequestExecutor(config.`com.gruchalski.consul.bind.port`).postJsonExecute(
              data,
              Path("/watch/keyprefix"),
              StatusCodes.OK,
              List(Authorization(OAuth2BearerToken(config.`com.gruchalski.consul.access-token`)))
            ), 1 second
          ) should matchPattern { case Left(_) => }
        }.orElse(fail("Expected configuration in the server."))
      }

      "given a valid /watch/checks request" in {
        val data = Json.toJson(
          List(
            ConsulModelHealthCheck(
              node = "support-node",
              checkId = "serfHealth",
              name = "Serf Health Status",
              status = "passing",
              notes = "",
              output = "Agent alive and reachable",
              serviceId = "",
              serviceName = ""
            )
          )
        )
        server.maybeConfiguration().map { config =>
          Await.result(
            RequestExecutor(config.`com.gruchalski.consul.bind.port`).postJsonExecute(
              data,
              Path("/watch/checks"),
              StatusCodes.OK,
              List(Authorization(OAuth2BearerToken(config.`com.gruchalski.consul.access-token`)))
            ), 1 second
          ) should matchPattern { case Left(_) => }
        }.orElse(fail("Expected configuration in the server."))
      }

      "given a valid /watch/event request" in {
        val data = Json.toJson(
          List(
            ConsulModelEvent(
              id = "41a95054-3db7-2d0b-64c3-32a82995a089",
              name = "event-name",
              nodeFilter = "",
              serviceFilter = "",
              tagFilter = "",
              version = 1,
              ltime = 2,
              payload = Some("c29tZS1kYXRh")
            )
          )
        )
        server.maybeConfiguration().map { config =>
          Await.result(
            RequestExecutor(config.`com.gruchalski.consul.bind.port`).postJsonExecute(
              data,
              Path("/watch/event"),
              StatusCodes.OK,
              List(Authorization(OAuth2BearerToken(config.`com.gruchalski.consul.access-token`)))
            ), 1 second
          ) should matchPattern { case Left(_) => }
        }.orElse(fail("Expected configuration in the server."))
      }

    }

  }

}
