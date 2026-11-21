import Dependencies.*

lazy val root = (project in file("."))
  .enablePlugins(JavaAppPackaging)
  .settings(
    organization := "su.wps",
    name := "wps-blog",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := "2.13.18",
    // sbt 2 moved the default target to target/out/jvm/scala-<ver>/<project>;
    // pin the staged output so the Dockerfile path stays stable across Scala bumps
    Universal / target := baseDirectory.value / "target" / "universal",
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
      caffeine,
      otel4sSdkMetrics,
      otel4sSdkExporterPrometheus,
      http4sOtel4sMetrics,
      logstashLogbackEncoder,
      distageCore,
      scalacheckShapeless % Test,
      testcontainersScala % Test,
      testcontainersPostgresql % Test,
      specs2 % Test,
      catsEffectTestingSpecs2 % Test,
      distageFramework
    ),
    // tofu-doobie 0.14.3 is built against doobie 1.0.0-RC13, and every doobie class and member
    // it references resolves in RC14. Remove once tofu releases against RC14 or later.
    libraryDependencySchemes += "org.typelevel" %% "doobie-core" % VersionScheme.Always,
    addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.13.4" cross CrossVersion.patch),
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
  )
