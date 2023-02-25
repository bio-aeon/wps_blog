import java.net.URLClassLoader

import Dependencies._
import sbt.complete.Parsers.spaceDelimited

lazy val root = (project in file("."))
  .settings(
    organization := "su.wps",
    name := "wps-blog",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := "2.13.10",
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
      http4sBlazeServer,
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
      pgMigrationsScala,
      scalacheckShapeless % Test,
      testcontainersScala % Test,
      testcontainersPostgresql % Test,
      specs2 % Test
    )
  )
  .settings(registerMigrateTask("su.wps.blog.tasks.MigrateTask"))

def registerMigrateTask[T](taskClass: String) = {
  val migrate = inputKey[Unit]("Migration task.")
  migrate := {
    val args: Seq[String] = spaceDelimited("<arg>").parsed

    val deps = (dependencyClasspath in Runtime).value
    val classLoader = new URLClassLoader(deps.map(_.data.toURI.toURL).toArray, null)
    val classType = classLoader.loadClass(taskClass)
    val task = classType.getConstructors.head
      .newInstance(args.mkString(" "))
      .asInstanceOf[Runnable]
    task.run()
  }
}