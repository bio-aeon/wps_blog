package su.wps.blog.repositories.sql

import derevo.derive
import su.wps.blog.models.domain.{PostId, Tag, TagId}
import tofu.higherKind.derived.representableK

@derive(representableK)
trait TagSql[DB[_]] {
  def findByPostId(postId: PostId): DB[List[Tag]]

  def findByPostIds(postIds: List[PostId]): DB[List[(PostId, Tag)]]

  def findAll: DB[List[Tag]]

  def findAllWithPostCounts: DB[List[(Tag, Int)]]

  def findById(id: TagId): DB[Option[Tag]]
}
