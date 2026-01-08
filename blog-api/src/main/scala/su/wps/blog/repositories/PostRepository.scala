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

  def incrementViews(id: PostId): DB[Int]

  // Full-text search methods
  def searchPosts(query: String, limit: Int, offset: Int): DB[List[Post]]

  def searchPostsCount(query: String): DB[Int]

  def findRecent(count: Int): DB[List[Post]]
}
