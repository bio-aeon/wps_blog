package su.wps.blog.utils

import com.typesafe.config.Config
import su.wps.pgmigrations.{ConnectionBuilder, DatabaseAdapter, Migrator, Vendor}

trait MigrationUtils {
  protected val config: Config

  protected def createMigrator() = {
    val driverClassName = config.getString("db.driver")
    val vendor = Vendor.forDriver(driverClassName)
    val migrationAdapter = DatabaseAdapter.forVendor(vendor, None)
    Class.forName(driverClassName)
    val url = config.getString("db.url")

    val username = config.getString("db.username")
    val password = config.getString("db.password")

    val connectionBuilder = new ConnectionBuilder(url, username, password)
    new Migrator(connectionBuilder, migrationAdapter)
  }
}
