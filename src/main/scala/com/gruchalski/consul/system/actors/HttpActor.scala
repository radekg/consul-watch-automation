package com.gruchalski.consul.system.actors

import akka.actor.{Actor, ActorLogging}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.http.scaladsl.model.HttpMethods.POST
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.gruchalski.consul.models._
import com.gruchalski.consul.system.actors.HttpActorUtils.Exceptions.JsonParseFailed
import com.gruchalski.consul.system.config.Configuration
import play.api.libs.json.{Json, Reads}

import scala.concurrent.Future
import scala.util.{Success, Try}

object HttpActorUtils {
  sealed trait Protocol
  final case class Bind() extends Protocol

  final case class State(val f: Option[Future[Http.ServerBinding]])

  object Exceptions {
    case class UnknownWatchType(val message: String) extends Exception(message)
    case class InvalidInput(val message: String) extends Exception(message)
    case class InvalidContentType(val message: String) extends Exception(message)
    case class JsonParseFailed(val message: String) extends Exception(message)
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
      ).getOrElse(uri.path, (e: HttpEntity) => {
        Future { Right(HttpActorUtils.Exceptions.UnknownWatchType("Unknown watch type.")) }
      })(entity).map({
        case Left(data) =>
          // process the data here...
          HttpResponse(StatusCodes.OK)
        case Right(cause) =>
          val status = cause match {
            case _: HttpActorUtils.Exceptions.UnknownWatchType   => StatusCodes.NotFound
            case _: HttpActorUtils.Exceptions.InvalidInput       => StatusCodes.BadRequest
            case _: HttpActorUtils.Exceptions.InvalidContentType => StatusCodes.BadRequest
            case _: HttpActorUtils.Exceptions.JsonParseFailed    => StatusCodes.InternalServerError
            case anyOther                                        => StatusCodes.InternalServerError
          }
          HttpResponse(status, entity=cause.getMessage)
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
      val bindingFuture = Http().bindAndHandleAsync(requestHandler,
        config.`com.gruchalski.consul.bind.host`,
        config.`com.gruchalski.consul.bind.port`)
      log.info("Server bound...")
      context.become(bound(state.copy(f = Some(bindingFuture))))
  }

  def bound(state: HttpActorUtils.State): Receive = {
    case _ =>
  }

  private def handleEntity[T](entity: HttpEntity)(implicit tjs: Reads[T]): Future[Either[T, Throwable]] = {
    Unmarshal(entity).to[String].flatMap { s =>
      Try {
        if (entity.getContentType() == ContentTypes.`application/json`) {
          Json.parse(s).asOpt[T] match {
            case Some(data) =>
              Future {
                Left(data)
              }
            case None =>
              Future {
                Right(HttpActorUtils.Exceptions.InvalidInput("Input could not be parsed to the format expected by the URI."))
              }
          }
        } else {
          Future { Right(HttpActorUtils.Exceptions.InvalidContentType("Request not application/json.")) }
        }
      }.getOrElse(Future { Right(JsonParseFailed("Error while parsing JSON.")) })
    }
  }

}
