import Dependencies._

lazy val root = (project in file("."))
  .settings(
    organization := "su.wps",
    name := "wps-blog",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := "2.13.11",
    scalacOptions ++= Seq(
      "-deprecation",
      "-encoding",
      "UTF-8",
      "-language:implicitConversions",
      "-language:higherKinds",
      "-language:postfixOps",
      "-feature",
      "-Xfatal-warnings",
      "-Ymacro-annotations"
    ),
    libraryDependencies ++= Seq(
      http4sEmberServer,
      http4sCirce,
      http4sDsl,
      circeParser,
      doobieCore,
      doobiePostgres,
      doobieHikari,
      tofuDerivation,
      tofuDoobie,
      logbackClassic,
      typesafeConfig,
      log4cats,
      log4catsSlf4j,
      mouse,
      chimney,
      fly4sCore,
      scalacheckShapeless % Test,
      testcontainersScala % Test,
      testcontainersPostgresql % Test,
      specs2 % Test
    ),
    addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.13.2" cross CrossVersion.patch),
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
  )
