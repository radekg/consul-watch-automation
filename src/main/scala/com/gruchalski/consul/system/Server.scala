package com.gruchalski.consul.system

import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.gruchalski.consul.system.actors.HttpActor
import com.typesafe.scalalogging.Logger

import scala.concurrent.duration._

case class Server() {

  implicit val system = ActorSystem("cdf")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher
  implicit val timeout = Timeout(5 seconds)

  private val log = Logger(getClass)

  def start(): Unit = {
    system.actorOf(Props(new HttpActor))
  }

  def stop(): Unit = {
    log.info("Stopping the system...")
    system.terminate() match {
      case _ =>
        log.info("System terminated. Bye ... \\o/")
    }
  }

  def withShutdownHook(): Server = {
    sys.addShutdownHook {
      log.info("Shutdown hook triggered. Going to stop...")
      this.stop()
    }
    this
  }

}
