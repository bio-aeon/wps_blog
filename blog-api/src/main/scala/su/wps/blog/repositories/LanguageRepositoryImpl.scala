package su.wps.blog.repositories

import su.wps.blog.models.domain.Language
import su.wps.blog.repositories.sql.{LanguageSql, LanguageSqlImpl}
import tofu.doobie.LiftConnectionIO

final class LanguageRepositoryImpl[DB[_]] private (sql: LanguageSql[DB])
    extends LanguageRepository[DB] {

  def findActive: DB[List[Language]] = sql.findActive
  def findDefault: DB[Option[Language]] = sql.findDefault
  def findByCode(code: String): DB[Option[Language]] = sql.findByCode(code)
}

object LanguageRepositoryImpl {
  def create[DB[_]: LiftConnectionIO]: LanguageRepositoryImpl[DB] = {
    val sql = LanguageSqlImpl.create[DB]
    new LanguageRepositoryImpl[DB](sql)
  }
}
