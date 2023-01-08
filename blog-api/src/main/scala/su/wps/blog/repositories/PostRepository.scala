package su.wps.blog.repositories

import su.wps.blog.models.Post

trait PostRepository[F[_]] {
  def findAllWithLimitAndOffset(limit: Int, offset: Int): F[List[Post]]
}
