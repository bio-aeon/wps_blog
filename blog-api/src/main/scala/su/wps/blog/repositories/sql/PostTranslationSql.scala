package su.wps.blog.repositories.sql

import derevo.derive
import su.wps.blog.models.domain.{PostId, PostTranslation}
import tofu.higherKind.derived.representableK

@derive(representableK)
trait PostTranslationSql[DB[_]] {
  def findByPostAndLanguage(postId: PostId, lang: String): DB[Option[PostTranslation]]
  def findByPostId(postId: PostId): DB[List[PostTranslation]]
  def findAvailableLanguages(postId: PostId): DB[List[String]]
  def findAvailableLanguagesByPostIds(postIds: List[PostId]): DB[Map[PostId, List[String]]]
}
