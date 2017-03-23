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

package com.gruchalski.consul.system

import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.gruchalski.consul.system.actors.HttpActor
import com.gruchalski.consul.system.config.Configuration
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.Logger

import scala.concurrent.duration._

case class Server() {

  implicit val system = ActorSystem("cdf")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher
  implicit val timeout = Timeout(5 seconds)

  private val log = Logger(getClass)

  private var configuration: Option[Configuration] = None

  def maybeConfiguration(): Option[Configuration] = configuration

  def start(config: Option[Config] = None): Unit = {
    maybeConfiguration().headOption match {
      case Some(_) => log.debug("Server already started.")
      case None =>
        val cfgInst = Configuration(config.getOrElse(ConfigFactory.load().resolve()))
        configuration = Some(cfgInst)
        log.info(s"Starting the system with configuration: ${configuration}")
        system.actorOf(Props(new HttpActor(cfgInst)))
    }
  }

  def stop(): Unit = {
    configuration = None
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
