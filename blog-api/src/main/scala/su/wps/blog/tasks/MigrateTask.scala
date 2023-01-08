package su.wps.blog.tasks

import org.slf4j.LoggerFactory
import su.wps.blog.utils.MigrationUtils
import su.wps.pgmigrations.{InstallAllMigrations, MigrateToVersion}

class MigrateTask(argStr: String) extends Task(argStr) with Runnable with MigrationUtils {
  private val commands = Set("to")

  private val log = LoggerFactory.getLogger(classOf[MigrateTask])

  val packages = Seq(
    (getClass.getPackage.getName.split("\\.").dropRight(1) :+ "migrations").mkString(".")
  ) // hack

  def run(): Unit = {
    val (method, params) = resolve
    method match {
      case "migrate" => migrate()
      case "to" =>
        val version = params.get("arg")
        migrateToVersion(version)

      case _ => throw new UnsupportedOperationException("No such operation.")
    }
  }

  private def migrate(): Unit = {
    val migrator = createMigrator()
    migrator.migrate(InstallAllMigrations, packages, searchSubPackages = false)
    log.info("Migrated up successfully.")
  }

  private def migrateToVersion(version: String): Unit = {
    val migrator = createMigrator()
    migrator.migrate(MigrateToVersion(version.toLong), packages, searchSubPackages = false)
    log.info(s"Migrated to version $version successfully.")
  }

  private def resolve: (String, Option[Map[String, String]]) =
    if (args.isEmpty) {
      ("migrate", None)
    } else if (args.length == 1) {
      ("arg", None)
    } else if (args.length == 2) {
      val command = args(0)
      if (commands.contains(command)) {
        (command, Some(Map("arg" -> args(1))))
      } else {
        throw new IllegalArgumentException("Invalid arguments.")
      }
    } else {
      throw new IllegalArgumentException("Invalid arguments.")
    }
}
