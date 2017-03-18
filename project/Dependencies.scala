import sbt._

object Version {
  final val akka = "2.4.17"
  final val akkaHttp = "10.0.4"
  final val commonsLang = "3.5"
  final val logback = "1.1.3"
  final val playJson = "2.6.0-M5"
  final val scalaLogging = "3.5.0"
  final val scalTest = "3.0.1"
}

object Library {

  val akkaActor: ModuleID = "com.typesafe.akka" %% "akka-actor" % Version.akka
  val akkaSlf4j: ModuleID = "com.typesafe.akka" %% "akka-slf4j" % Version.akka
  val akkaHttp: ModuleID = "com.typesafe.akka" %% "akka-http" % Version.akkaHttp
  val akkaTestKit: ModuleID = "com.typesafe.akka" %% "akka-testkit" % Version.akka

  val commonsLang: ModuleID = "org.apache.commons" % "commons-lang3" % Version.commonsLang
  val logback: ModuleID = "ch.qos.logback" % "logback-classic" % Version.logback
  val playJson: ModuleID =  "com.typesafe.play" %% "play-json" % Version.playJson
  val scalaLogging: ModuleID = "com.typesafe.scala-logging" % "scala-logging_2.12" % Version.scalaLogging

  val scalaTest: ModuleID = "org.scalatest" %% "scalatest" % Version.scalTest
}

object DependencyGroups {

  val akka = Seq(Library.akkaActor, Library.akkaHttp, Library.akkaSlf4j)
  val apacheCommons = Seq(Library.commonsLang)
  val logging = Seq(Library.logback, Library.scalaLogging)
  val unitTests = Seq(Library.scalaTest % "test", Library.akkaTestKit % "test")

}