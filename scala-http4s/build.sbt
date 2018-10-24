import Dependencies._

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
      http4sBlazeServer,
      http4sCirce,
      http4sDsl,
      circeParser,
      circeOptics,
      logbackClassic,
      sangria,
      sangriaCirce,
      pgMigrationsScala,
      specs2 % "test"
    )
  )
