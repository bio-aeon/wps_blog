package su.wps.blog.repositories

import su.wps.blog.models.domain.{PostId, PostTranslation}
import su.wps.blog.repositories.sql.{PostTranslationSql, PostTranslationSqlImpl}
import tofu.doobie.LiftConnectionIO

final class PostTranslationRepositoryImpl[DB[_]] private (sql: PostTranslationSql[DB])
    extends PostTranslationRepository[DB] {

  def findByPostAndLanguage(postId: PostId, lang: String): DB[Option[PostTranslation]] =
    sql.findByPostAndLanguage(postId, lang)

  def findByPostId(postId: PostId): DB[List[PostTranslation]] =
    sql.findByPostId(postId)

  def findAvailableLanguages(postId: PostId): DB[List[String]] =
    sql.findAvailableLanguages(postId)

  def findAvailableLanguagesByPostIds(postIds: List[PostId]): DB[Map[PostId, List[String]]] =
    sql.findAvailableLanguagesByPostIds(postIds)
}

object PostTranslationRepositoryImpl {
  def create[DB[_]: LiftConnectionIO]: PostTranslationRepositoryImpl[DB] = {
    val sql = PostTranslationSqlImpl.create[DB]
    new PostTranslationRepositoryImpl[DB](sql)
  }
}
