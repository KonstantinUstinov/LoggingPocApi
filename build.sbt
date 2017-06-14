name := "LoggingPocApi"

organization := "us.ygrene"

version := "1.0-SNAPSHOT"

scalaVersion := "2.12.1"

scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-target:jvm-1.8",
  "-encoding", "utf8",
  "-Xfuture",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-unused"
)


lazy val akkaHttpVersion = "10.0.7"

libraryDependencies += "com.typesafe.akka" %% "akka-http" % akkaHttpVersion
libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion
libraryDependencies += "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % "test"
libraryDependencies += "org.reactivemongo" %% "reactivemongo" % "0.12.3"
libraryDependencies += "org.reactivemongo" %% "reactivemongo-bson" % "0.12.3"
libraryDependencies += "org.specs2" %% "specs2-core" % "3.9.0" % "test"
libraryDependencies += "org.slf4j" % "slf4j-log4j12" % "1.5.3"
libraryDependencies += "joda-time" % "joda-time" % "2.9.9"

fork in Test := true
javaOptions in Test += "-Dconfig.resource=local/env.conf"