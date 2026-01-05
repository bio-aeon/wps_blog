package su.wps.blog.repositories

import su.wps.blog.models.domain.{Post, PostId}

trait PostRepository[DB[_]] {
  def findAllWithLimitAndOffset(limit: Int, offset: Int): DB[List[Post]]

  def findCount: DB[Int]

  def findById(id: PostId): DB[Option[Post]]

  // Admin methods - include hidden posts
  def findAllWithLimitAndOffsetIncludeHidden(limit: Int, offset: Int): DB[List[Post]]

  def findCountIncludeHidden: DB[Int]

  def findByTagSlug(tagSlug: String, limit: Int, offset: Int): DB[List[Post]]

  def findCountByTagSlug(tagSlug: String): DB[Int]
}
