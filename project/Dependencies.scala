import sbt._

object Version {
  final val commonsLang = "3.5"
  final val playJson = "2.6.0-M5"
  final val scalTest = "3.0.1"
}

object Library {
  val commonsLang: ModuleID = "org.apache.commons" % "commons-lang3" % Version.commonsLang
  val playJson: ModuleID =  "com.typesafe.play" %% "play-json" % Version.playJson
  val scalaTest: ModuleID = "org.scalatest" %% "scalatest" % Version.scalTest
}

object DependencyGroups {

  val apacheCommons = Seq(Library.commonsLang)
  val unitTests = Seq(Library.scalaTest % "test")

}