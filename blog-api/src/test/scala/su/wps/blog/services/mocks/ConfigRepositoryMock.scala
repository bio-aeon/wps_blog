package su.wps.blog.services.mocks

import cats.Applicative
import cats.syntax.applicative.*
import su.wps.blog.models.domain.Config
import su.wps.blog.repositories.ConfigRepository

object ConfigRepositoryMock {
  def create[DB[_]: Applicative](
    findByNameResult: Option[Config] = None,
    findByNamesResult: List[Config] = Nil
  ): ConfigRepository[DB] = new ConfigRepository[DB] {
    def findByName(name: String): DB[Option[Config]] =
      findByNameResult.pure[DB]

    def findByNames(names: List[String]): DB[List[Config]] =
      findByNamesResult.pure[DB]
  }
}
