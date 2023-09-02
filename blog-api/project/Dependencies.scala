import sbt._

object Dependencies {

  object Versions {
    val http4s = "0.23.22"
    val specs2 = "4.20.0"
    val logback = "1.4.8"
    val circe = "0.14.3"
    val tofu = "0.12.0.1"
    val typesafeConfig = "1.4.2"
    val pureconfig = "0.17.4"
    val doobie = "1.0.0-RC2"
    val log4cats = "2.6.0"
    val mouse = "1.2.1"
    val chimney = "0.7.5"
    val fly4s = "0.0.18"
    val scalacheckShapeless = "1.3.0"
    val testcontainersScala = "0.40.17"
    val testcontainersPostgresql = "1.18.3"
  }

  val http4sEmberServer = "org.http4s" %% "http4s-ember-server" % Versions.http4s
  val http4sCirce = "org.http4s" %% "http4s-circe" % Versions.http4s
  val http4sDsl = "org.http4s" %% "http4s-dsl" % Versions.http4s
  val circeParser = "io.circe" %% "circe-parser" % Versions.circe
  val circeGeneric = "io.circe" %% "circe-generic" % Versions.circe
  val circeExtras = "io.circe" %% "circe-generic-extras" % Versions.circe
  val doobieCore = "org.tpolecat" %% "doobie-core" % Versions.doobie
  val doobiePostgres = "org.tpolecat" %% "doobie-postgres" % Versions.doobie
  val doobieHikari = "org.tpolecat" %% "doobie-hikari" % Versions.doobie
  val tofuDerivation = "tf.tofu" %% "tofu-derivation" % Versions.tofu
  val tofuDoobie = "tf.tofu" %% "tofu-doobie-ce3" % Versions.tofu
  val logbackClassic = "ch.qos.logback" % "logback-classic" % Versions.logback
  val typesafeConfig = "com.typesafe" % "config" % Versions.typesafeConfig
  val pureconfig = "com.github.pureconfig" %% "pureconfig" % Versions.pureconfig
  val log4cats = "org.typelevel" %% "log4cats-core" % Versions.log4cats
  val log4catsSlf4j = "org.typelevel" %% "log4cats-slf4j" % Versions.log4cats
  val mouse = "org.typelevel" %% "mouse" % Versions.mouse
  val chimney = "io.scalaland" %% "chimney" % Versions.chimney
  val fly4sCore = "com.github.geirolz" %% "fly4s-core" % Versions.fly4s
  val scalacheckShapeless = "com.github.alexarchambault" %% "scalacheck-shapeless_1.15" % Versions.scalacheckShapeless
  val testcontainersScala = "com.dimafeng" %% "testcontainers-scala" % Versions.testcontainersScala
  val testcontainersPostgresql = "org.testcontainers" % "postgresql" % Versions.testcontainersPostgresql
  val specs2 = "org.specs2" %% "specs2-core" % Versions.specs2
}
