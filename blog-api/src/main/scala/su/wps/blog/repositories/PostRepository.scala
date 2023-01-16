package su.wps.blog.repositories

import su.wps.blog.models.Post

trait PostRepository[DB[_]] {
  def findAllWithLimitAndOffset(limit: Int, offset: Int): DB[List[Post]]
}
