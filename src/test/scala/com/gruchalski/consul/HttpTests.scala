package com.gruchalski.consul

import java.net.ServerSocket

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import com.gruchalski.consul.system.Server
import com.typesafe.config.{ConfigFactory, ConfigValueFactory}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.Try

class HttpTests extends WordSpec with Matchers with BeforeAndAfterAll {

  import scala.concurrent.ExecutionContext.Implicits.global

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
  private val config = ConfigFactory.load().resolve()
    .withValue("com.gruchalski.consul.bind.port", ConfigValueFactory.fromAnyRef(
      freePortSafe().getOrElse(fail("Failed to bind, test can't be executed."))
    ))

  override def beforeAll {
    server.start(Some(config))
  }

  override def afterAll {
    server.stop()
    system.terminate()
  }

  "HTTP server" must {

    "return NotFound" when {

      "requesting a response from a non-existing URI" in {

        server.maybeConfiguration().map { config =>
          val source = Source.single(HttpRequest(
            uri = Uri(path = Path("/non/existing/uri")),
            method = HttpMethods.GET,
            entity = HttpEntity("a request to a non-existing URI")
          ))
          val flow = Http().outgoingConnection("localhost", config.`com.gruchalski.consul.bind.port`).mapAsync(1) { response =>
            response.status match {
              case StatusCodes.OK        => Future(Right("Expected the request to fail."))
              case StatusCodes.NotFound  => Future(Left("ok"))
              case anyOther              => Future(Right(s"Unexpected status code: $anyOther"))
            }
          }
          Await.result(source.via(flow).runWith(Sink.head), 5 seconds) should matchPattern {
            case Left(_) =>
          }
        }.orElse(fail("Expected configuration in the server."))

      }

      "requesting to process an unknown watch type" in {

        server.maybeConfiguration().map { config =>
          val source = Source.single(HttpRequest(
            uri = Uri(path = Path("/watch/unknown-type")),
            method = HttpMethods.POST,
            entity = HttpEntity(ContentType(MediaTypes.`application/json`), """{"json": "data"}""")
          ))
          val flow = Http().outgoingConnection("localhost", config.`com.gruchalski.consul.bind.port`).mapAsync(1) { response =>
            response.status match {
              case StatusCodes.OK        => Future(Right("Expected the request to fail."))
              case StatusCodes.NotFound  => Future(Left("ok"))
              case anyOther              => Future(Right(s"Unexpected status code: $anyOther"))
            }
          }
          Await.result(source.via(flow).runWith(Sink.head), 5 seconds) should matchPattern {
            case Left(_) =>
          }
        }.orElse(fail("Expected configuration in the server."))

      }

    }

  }

}
