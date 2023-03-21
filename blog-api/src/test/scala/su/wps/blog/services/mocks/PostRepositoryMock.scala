package su.wps.blog.services.mocks

import cats.Applicative
import su.wps.blog.models.domain.{Post, PostId}
import su.wps.blog.repositories.PostRepository

object PostRepositoryMock {
  def create[DB[_]](
    findAllResult: List[Post] = Nil,
    findCountResult: Int = 0,
    findByIdResult: Option[Post] = None
  )(implicit DB: Applicative[DB]): PostRepository[DB] = new PostRepository[DB] {
    def findAllWithLimitAndOffset(limit: Int, offset: Int): DB[List[Post]] =
      DB.pure(findAllResult)

    def findCount: DB[Int] = DB.pure(findCountResult)

    def findById(id: PostId): DB[Option[Post]] = DB.pure(findByIdResult)
  }
}
