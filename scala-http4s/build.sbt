val Http4sVersion = "0.18.16"
val Specs2Version = "4.3.4"
val LogbackVersion = "1.2.3"

lazy val root = (project in file("."))
  .settings(
    organization := "su.wps",
    name := "wps-blog",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := "2.12.6",
    scalacOptions ++= Seq(
      "-deprecation",
      "-feature",
      "-language:implicitConversions",
      "-language:postfixOps",
      "-language:higherKinds",
      "-language:reflectiveCalls",
      "-Ypartial-unification"
    ),
    libraryDependencies ++= Seq(
      "org.http4s"      %% "http4s-blaze-server" % Http4sVersion,
      "org.http4s"      %% "http4s-circe"        % Http4sVersion,
      "org.http4s"      %% "http4s-dsl"          % Http4sVersion,
      "org.specs2"     %% "specs2-core"          % Specs2Version % "test",
      "ch.qos.logback"  %  "logback-classic"     % LogbackVersion
    )
  )
