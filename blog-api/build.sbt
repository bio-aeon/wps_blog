import Dependencies.*

lazy val root = (project in file("."))
  .settings(
    organization := "su.wps",
    name := "wps-blog",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := "2.13.16",
    scalacOptions ++= Seq(
      "-deprecation",
      "-encoding",
      "UTF-8",
      "-language:implicitConversions",
      "-language:higherKinds",
      "-language:postfixOps",
      "-feature",
      "-Xfatal-warnings",
      "-Ymacro-annotations",
      "-Xsource:3"
    ),
    libraryDependencies ++= Seq(
      http4sEmberServer,
      http4sCirce,
      http4sDsl,
      circeParser,
      circeGeneric,
      doobieCore,
      doobiePostgres,
      doobieHikari,
      tofuDerivation,
      tofuDoobie,
      logbackClassic,
      typesafeConfig,
      pureconfig,
      log4cats,
      log4catsSlf4j,
      mouse,
      chimney,
      fly4s,
      flywayPostgresql,
      tapirJsonCirce,
      tapirHttp4sServer,
      tapirSwaggerUiBundle,
      scalacheckShapeless % Test,
      testcontainersScala % Test,
      testcontainersPostgresql % Test,
      specs2 % Test
    ),
    addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.13.3" cross CrossVersion.patch),
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
  )
