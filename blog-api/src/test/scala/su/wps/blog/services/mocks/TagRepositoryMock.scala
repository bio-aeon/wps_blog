package su.wps.blog.services.mocks

import cats.Applicative
import su.wps.blog.models.domain.{PostId, Tag, TagId}
import su.wps.blog.repositories.TagRepository

object TagRepositoryMock {
  def create[DB[_]](
    findByPostIdResult: List[Tag] = Nil,
    findByPostIdsResult: List[(PostId, Tag)] = Nil,
    findAllResult: List[Tag] = Nil,
    findAllWithPostCountsResult: List[(Tag, Int)] = Nil,
    findByIdResult: Option[Tag] = None
  )(implicit DB: Applicative[DB]): TagRepository[DB] = new TagRepository[DB] {
    def findByPostId(postId: PostId): DB[List[Tag]] =
      DB.pure(findByPostIdResult)

    def findByPostIds(postIds: List[PostId]): DB[List[(PostId, Tag)]] =
      DB.pure(findByPostIdsResult)

    def findAll: DB[List[Tag]] =
      DB.pure(findAllResult)

    def findAllWithPostCounts: DB[List[(Tag, Int)]] =
      DB.pure(findAllWithPostCountsResult)

    def findById(id: TagId): DB[Option[Tag]] =
      DB.pure(findByIdResult)
  }
}
