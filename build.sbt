name := "consul-watch-automation"

version := "1.0"

scalaVersion := "2.12.1"

antlr4Settings
antlr4GenListener in Antlr4 := true
antlr4GenVisitor in Antlr4 := false
antlr4PackageName in Antlr4 := Some("com.gruchalski.consul.cdf")

libraryDependencies ++= DependencyGroups.akka
libraryDependencies ++= DependencyGroups.apacheCommons
libraryDependencies ++= DependencyGroups.logging
libraryDependencies ++= Seq(Library.playJson)
libraryDependencies ++= DependencyGroups.unitTests

mainClass in (Compile, run) := Some("com.gruchalski.consul.system.Run")