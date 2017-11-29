name := "room-reserve"

version := "1.0"

scalaVersion := "2.12.2"

lazy val akkaVersion = "2.5.3"
lazy val flash = taskKey[Unit]("Flash each configured device with roomReserve binary.")

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  "com.microsoft.ews-java-api" % "ews-java-api" % "2.0",
  "com.typesafe.play" %% "play-ahc-ws-standalone" % "1.1.3",
  "com.typesafe.play" %% "play-ws-standalone-json" % "1.1.3",
  "com.typesafe.play" %% "play-json" % "2.6.7",
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.9.2",
  "com.fasterxml.jackson.core" % "jackson-core" % "2.1.1",
  "com.fasterxml.jackson.dataformat" % "jackson-dataformat-yaml" % "2.9.2",
  "org.slf4j" % "slf4j-simple" % "1.6.4"
)