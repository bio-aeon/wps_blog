package su.wps.blog.repositories

import su.wps.blog.models.domain.TagId
import su.wps.blog.repositories.sql.{TagTranslationSql, TagTranslationSqlImpl}
import tofu.doobie.LiftConnectionIO

final class TagTranslationRepositoryImpl[DB[_]] private (sql: TagTranslationSql[DB])
    extends TagTranslationRepository[DB] {

  def findByTagIds(tagIds: List[TagId], lang: String): DB[Map[TagId, String]] =
    sql.findByTagIds(tagIds, lang)

  def findAllTranslatedNames(lang: String): DB[Map[TagId, String]] =
    sql.findAllTranslatedNames(lang)
}

object TagTranslationRepositoryImpl {
  def create[DB[_]: LiftConnectionIO]: TagTranslationRepositoryImpl[DB] = {
    val sql = TagTranslationSqlImpl.create[DB]
    new TagTranslationRepositoryImpl[DB](sql)
  }
}
