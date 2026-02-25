package su.wps.blog.repositories

import su.wps.blog.models.domain.Config
import su.wps.blog.repositories.sql.{ConfigSql, ConfigSqlImpl}
import tofu.doobie.LiftConnectionIO

final class ConfigRepositoryImpl[DB[_]] private (sql: ConfigSql[DB])
    extends ConfigRepository[DB] {
  def findByName(name: String): DB[Option[Config]] =
    sql.findByName(name)

  def findByNames(names: List[String]): DB[List[Config]] =
    sql.findByNames(names)
}

object ConfigRepositoryImpl {
  def create[DB[_]: LiftConnectionIO]: ConfigRepositoryImpl[DB] = {
    val configSql = ConfigSqlImpl.create[DB]
    new ConfigRepositoryImpl[DB](configSql)
  }
}
