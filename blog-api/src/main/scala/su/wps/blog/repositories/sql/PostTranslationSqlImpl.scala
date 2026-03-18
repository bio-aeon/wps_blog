package su.wps.blog.repositories.sql

import cats.syntax.applicative.*
import cats.syntax.list.*
import cats.tagless.syntax.functorK.*
import doobie.*
import doobie.implicits.*
import su.wps.blog.instances.time.*
import su.wps.blog.models.domain.{PostId, PostTranslation}
import tofu.doobie.LiftConnectionIO

final class PostTranslationSqlImpl private extends PostTranslationSql[ConnectionIO] {

  private val selectFields: Fragment =
    fr"""post_id, language_code, name, text, short_text,
         seo_title, seo_description, seo_keywords,
         translation_status, created_at, updated_at, id"""

  def findByPostAndLanguage(postId: PostId, lang: String): ConnectionIO[Option[PostTranslation]] =
    (fr"SELECT" ++ selectFields ++
      fr"FROM post_translations WHERE post_id = ${postId.value} AND language_code = $lang")
      .query[PostTranslation]
      .option

  def findByPostId(postId: PostId): ConnectionIO[List[PostTranslation]] =
    (fr"SELECT" ++ selectFields ++
      fr"FROM post_translations WHERE post_id = ${postId.value} ORDER BY language_code")
      .query[PostTranslation]
      .to[List]

  def findAvailableLanguages(postId: PostId): ConnectionIO[List[String]] =
    sql"""SELECT language_code FROM post_translations
          WHERE post_id = ${postId.value} AND translation_status = 'published'
          ORDER BY language_code"""
      .query[String]
      .to[List]

  def findAvailableLanguagesByPostIds(
    postIds: List[PostId]
  ): ConnectionIO[Map[PostId, List[String]]] =
    if (postIds.isEmpty) {
      Map.empty[PostId, List[String]].pure[ConnectionIO]
    } else {
      val ids = postIds.map(_.value)
      val inClause = Fragments.in(fr"post_id", ids.toNel.get)
      sql"""SELECT post_id, language_code FROM post_translations
            WHERE $inClause AND translation_status = 'published'
            ORDER BY post_id, language_code"""
        .query[(Int, String)]
        .to[List]
        .map(_.groupBy(_._1).map { case (pid, langs) =>
          PostId(pid) -> langs.map(_._2)
        })
    }
}

object PostTranslationSqlImpl {
  def create[DB[_]](implicit L: LiftConnectionIO[DB]): PostTranslationSql[DB] =
    new PostTranslationSqlImpl().mapK(L.liftF)
}
