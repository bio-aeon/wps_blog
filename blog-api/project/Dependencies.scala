import sbt.*

object Dependencies {

  object Versions {
    val http4s = "0.23.30"
    val specs2 = "4.21.0"
    val logback = "1.5.18"
    val circe = "0.14.13"
    val tofu = "0.13.7"
    val typesafeConfig = "1.4.3"
    val pureconfig = "0.17.9"
    val doobie = "1.0.0-RC5"
    val log4cats = "2.7.0"
    val mouse = "1.3.2"
    val chimney = "1.7.3"
    val fly4s = "1.1.0"
    val flyway = "11.8.0"
    val scalacheckShapeless = "1.3.1"
    val testcontainersScala = "0.43.0"
    val testcontainersPostgresql = "1.21.0"
  }

  val http4sEmberServer = "org.http4s" %% "http4s-ember-server" % Versions.http4s
  val http4sCirce = "org.http4s" %% "http4s-circe" % Versions.http4s
  val http4sDsl = "org.http4s" %% "http4s-dsl" % Versions.http4s
  val circeParser = "io.circe" %% "circe-parser" % Versions.circe
  val circeGeneric = "io.circe" %% "circe-generic" % Versions.circe
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
  val fly4s = "com.github.geirolz" %% "fly4s" % Versions.fly4s
  val flywayPostgresql = "org.flywaydb" % "flyway-database-postgresql" % Versions.flyway
  val scalacheckShapeless =
    "com.github.alexarchambault" %% "scalacheck-shapeless_1.16" % Versions.scalacheckShapeless
  val testcontainersScala = "com.dimafeng" %% "testcontainers-scala" % Versions.testcontainersScala
  val testcontainersPostgresql =
    "org.testcontainers" % "postgresql" % Versions.testcontainersPostgresql
  val specs2 = "org.specs2" %% "specs2-core" % Versions.specs2
}
