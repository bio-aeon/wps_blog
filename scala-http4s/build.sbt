import java.net.URLClassLoader

import Dependencies._
import sbt.complete.Parsers.spaceDelimited

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
      typesafeConfig,
      log4cats,
      log4catsSlf4j,
      pgMigrationsScala,
      specs2 % "test"
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
