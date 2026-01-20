package su.wps.blog.repositories

import su.wps.blog.models.domain.Page
import su.wps.blog.repositories.sql.{PageSql, PageSqlImpl}
import tofu.doobie.LiftConnectionIO

final class PageRepositoryImpl[DB[_]] private (sql: PageSql[DB]) extends PageRepository[DB] {
  def findByUrl(url: String): DB[Option[Page]] =
    sql.findByUrl(url)
}

object PageRepositoryImpl {
  def create[DB[_]: LiftConnectionIO]: PageRepositoryImpl[DB] = {
    val pageSql = PageSqlImpl.create[DB]
    new PageRepositoryImpl[DB](pageSql)
  }
}
