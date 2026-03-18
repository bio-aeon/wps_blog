package su.wps.blog.repositories.sql

import cats.tagless.syntax.functorK.*
import doobie.*
import doobie.implicits.*
import su.wps.blog.models.domain.Language
import tofu.doobie.LiftConnectionIO

final class LanguageSqlImpl private extends LanguageSql[ConnectionIO] {
  private val tableName: Fragment = Fragment.const("languages")

  def findActive: ConnectionIO[List[Language]] =
    (fr"SELECT code, name, native_name, is_default, is_active, sort_order FROM" ++ tableName ++
      fr"WHERE is_active = true ORDER BY sort_order")
      .query[Language]
      .to[List]

  def findDefault: ConnectionIO[Option[Language]] =
    (fr"SELECT code, name, native_name, is_default, is_active, sort_order FROM" ++ tableName ++
      fr"WHERE is_default = true AND is_active = true")
      .query[Language]
      .option

  def findByCode(code: String): ConnectionIO[Option[Language]] =
    (fr"SELECT code, name, native_name, is_default, is_active, sort_order FROM" ++ tableName ++
      fr"WHERE code = $code AND is_active = true")
      .query[Language]
      .option
}

object LanguageSqlImpl {
  def create[DB[_]](implicit L: LiftConnectionIO[DB]): LanguageSql[DB] =
    new LanguageSqlImpl().mapK(L.liftF)
}
