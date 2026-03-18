package su.wps.blog.repositories.sql

import cats.syntax.applicative.*
import cats.syntax.list.*
import cats.tagless.syntax.functorK.*
import doobie.*
import doobie.implicits.*
import su.wps.blog.models.domain.TagId
import tofu.doobie.LiftConnectionIO

final class TagTranslationSqlImpl private extends TagTranslationSql[ConnectionIO] {

  def findByTagIds(tagIds: List[TagId], lang: String): ConnectionIO[Map[TagId, String]] =
    if (tagIds.isEmpty) {
      Map.empty[TagId, String].pure[ConnectionIO]
    } else {
      val ids = tagIds.map(_.value)
      val inClause = Fragments.in(fr"tag_id", ids.toNel.get)
      sql"""SELECT tag_id, name FROM tag_translations
            WHERE $inClause AND language_code = $lang"""
        .query[(Int, String)]
        .to[List]
        .map(_.map { case (tid, name) => TagId(tid) -> name }.toMap)
    }

  def findAllTranslatedNames(lang: String): ConnectionIO[Map[TagId, String]] =
    sql"""SELECT tag_id, name FROM tag_translations WHERE language_code = $lang"""
      .query[(Int, String)]
      .to[List]
      .map(_.map { case (tid, name) => TagId(tid) -> name }.toMap)
}

object TagTranslationSqlImpl {
  def create[DB[_]](implicit L: LiftConnectionIO[DB]): TagTranslationSql[DB] =
    new TagTranslationSqlImpl().mapK(L.liftF)
}
