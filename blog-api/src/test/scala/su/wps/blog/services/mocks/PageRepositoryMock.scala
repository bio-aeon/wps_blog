package su.wps.blog.services.mocks

import cats.Applicative
import su.wps.blog.models.domain.Page
import su.wps.blog.repositories.PageRepository

object PageRepositoryMock {
  def create[DB[_]](
    findByUrlResult: Option[Page] = None,
    findAllResult: List[Page] = Nil
  )(implicit DB: Applicative[DB]): PageRepository[DB] = new PageRepository[DB] {
    def findByUrl(url: String): DB[Option[Page]] =
      DB.pure(findByUrlResult)

    def findAll: DB[List[Page]] =
      DB.pure(findAllResult)
  }
}
