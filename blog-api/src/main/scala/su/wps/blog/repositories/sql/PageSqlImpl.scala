package su.wps.blog.repositories.sql

import cats.tagless.syntax.functorK.*
import doobie.*
import doobie.implicits.*
import su.wps.blog.instances.time.*
import su.wps.blog.models.domain.Page
import tofu.doobie.LiftConnectionIO

final class PageSqlImpl private extends PageSql[ConnectionIO] {
  private val tableName: Fragment = Fragment.const("pages")

  def findByUrl(url: String): ConnectionIO[Option[Page]] =
    (fr"SELECT url, title, content, created_at, id FROM" ++ tableName ++ fr"WHERE url = $url")
      .query[Page]
      .option

  def findAll: ConnectionIO[List[Page]] =
    (fr"SELECT url, title, content, created_at, id FROM" ++ tableName ++ fr"ORDER BY title")
      .query[Page]
      .to[List]
}

object PageSqlImpl {
  def create[DB[_]](implicit L: LiftConnectionIO[DB]): PageSql[DB] =
    new PageSqlImpl().mapK(L.liftF)
}
