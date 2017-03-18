package com.gruchalski.consul.parser

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

final case class ExecAction(val path: String, val onlyIf: Option[String] = None) extends ConsulWatchAction

final case class SystemServiceAction(val action: SystemServiceActions.Action,
                                     val serviceName: String)
  extends ConsulWatchAction

final case class CreateConsulServiceAction(val serviceName: String,
                                           val params: Map[String, Any] = Map.empty[String, Any])
  extends ConsulWatchAction
final case class TemplateAction(val source: String,
                                val destination: String,
                                val params: Map[String, Any] = Map.empty[String, Any],
                                val via: Option[ExecAction] = None)
  extends ConsulWatchAction

case class ConsulServiceWatchRestriction(val role: Option[String],
                                         val actions: List[ConsulWatchAction] = List.empty[ConsulWatchAction])

case class ConsulServiceWatch(val count: Int,
                              val service: String,
                              val scopes: Map[Option[String], ConsulServiceWatchRestriction] = Map.empty[Option[String], ConsulServiceWatchRestriction])

case class LogDirective(val path: String)

case class ProgramTree(val roles: List[String],
                       val consulWatchTriggers: Map[Tuple2[Int, String], ConsulServiceWatch],
                       val log: Option[LogDirective] = None)