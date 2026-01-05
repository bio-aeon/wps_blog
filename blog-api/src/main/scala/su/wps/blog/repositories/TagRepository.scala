package su.wps.blog.repositories

import su.wps.blog.models.domain.{PostId, Tag, TagId}

trait TagRepository[DB[_]] {
  def findByPostId(postId: PostId): DB[List[Tag]]

  def findByPostIds(postIds: List[PostId]): DB[List[(PostId, Tag)]]

  def findAll: DB[List[Tag]]

  def findAllWithPostCounts: DB[List[(Tag, Int)]]

  def findById(id: TagId): DB[Option[Tag]]
}
