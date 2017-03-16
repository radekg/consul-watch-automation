package com.gruchalski.consul

import java.util

object ConsulSupportedScopes {
  val envScope = "env"
}

case class UnsupportedSystemActionException(val message: String) extends Exception(message)

object SystemServiceActions {
  sealed class Action(val action: String)
  final case object StartService extends Action("system_service_start")
  final case object StopService extends Action("system_service_stop")
  final case object RestartService extends Action("system_service_restart")
  final case object EnableService extends Action("system_service_enable")
  final case object DisableService extends Action("system_service_disable")

  def apply(action: String) = {
    if (action == StartService.action) StartService
    else if (action == StopService.action) StopService
    else if (action == RestartService.action) RestartService
    else if (action == EnableService.action) EnableService
    else if (action == DisableService.action) DisableService
    else
      throw new UnsupportedSystemActionException(s"Unsupported service action: $action.")
  }
}

sealed trait ConsulWatchAction

sealed trait ActionWithArguments extends ConsulWatchAction {
  val arguments: util.Map[String, Any]
}

final case class ExecAction(val path: String) extends ConsulWatchAction
final case class SystemServiceAction(val action: SystemServiceActions.Action, val serviceName: String) extends ConsulWatchAction
final case class CreateConsulServiceAction(val serviceName: String, val arguments: util.Map[String, Any] = new util.HashMap[String, Any]())
  extends ActionWithArguments
final case class TemplateAction(val source: String,
                                val destination: String,
                                val arguments: util.Map[String, Any] = new util.HashMap[String, Any](),
                                val via: Option[String] = None)
  extends ActionWithArguments

case class ConsulWatchTriggerScope(val role: Option[String], actions: util.List[ConsulWatchAction] = new util.ArrayList[ConsulWatchAction]())

case class ConsulWatchTrigger(val count: Int,
                              val service: String,
                              val scopes: util.Map[Option[String], ConsulWatchTriggerScope] = new util.HashMap[Option[String], ConsulWatchTriggerScope]())

case class ProgramTree(val roles: List[String], val consulWatchTriggers: Map[Tuple2[Int, String], ConsulWatchTrigger])