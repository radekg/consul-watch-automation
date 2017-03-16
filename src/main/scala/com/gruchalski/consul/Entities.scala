package com.gruchalski.consul

import java.util

object ConsulSupportedScopes {
  val envScope = "env"
}

sealed trait ConsulWatchAction

case class ConsulWatchTriggerScope(val role: Option[String], actions: util.List[ConsulWatchAction] = new util.ArrayList[ConsulWatchAction]())

case class ConsulWatchTrigger(val count: Int,
                              val service: String,
                              val actons: util.List[ConsulWatchTriggerScope] = new util.ArrayList[ConsulWatchTriggerScope]())
