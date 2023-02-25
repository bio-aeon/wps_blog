package su.wps.blog.tools

import cats.effect.IO
import com.dimafeng.testcontainers.PostgreSQLContainer
import com.typesafe.config.{Config, ConfigFactory}
import doobie.Transactor
import org.specs2.specification.{BeforeAfterAll, BeforeAfterEach}
import su.wps.blog.utils.MigrationUtils
import su.wps.pgmigrations.{InstallAllMigrations, Migrator, RemoveAllMigrations}

import scala.jdk.CollectionConverters._

trait DbTest extends BeforeAfterEach with BeforeAfterAll with MigrationUtils {

  private lazy val container: PostgreSQLContainer = PostgreSQLContainer()

  protected lazy val config: Config =
    ConfigFactory.parseMap(
      Map(
        "db" -> Map(
          "driver" -> container.driverClassName,
          "url" -> container.jdbcUrl,
          "username" -> container.username,
          "password" -> container.password
        ).asJava
      ).asJava
    )

  implicit lazy val xa: Transactor[IO] = {
    Transactor.fromDriverManager[IO](
      container.driverClassName,
      container.jdbcUrl,
      container.username,
      container.password
    )
  }

  private lazy val migrator: Migrator = createMigrator()

  private val migrationPackages = Seq(
    (getClass.getPackage.getName.split("\\.").dropRight(1) :+ "migrations").mkString(".")
  )

  def beforeAll(): Unit =
    container.start()

  def afterAll(): Unit =
    container.stop()

  override def before: Any =
    migrator.migrate(InstallAllMigrations, migrationPackages, searchSubPackages = false)

  override protected def after: Any =
    migrator.migrate(RemoveAllMigrations, migrationPackages, searchSubPackages = false)
}
