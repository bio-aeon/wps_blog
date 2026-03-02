package su.wps.blog.services.mocks

import cats.Applicative
import cats.syntax.applicative.*
import su.wps.blog.models.domain.SocialLink
import su.wps.blog.repositories.SocialLinkRepository

object SocialLinkRepositoryMock {
  def create[DB[_]: Applicative](
    findAllActiveResult: List[SocialLink] = Nil
  ): SocialLinkRepository[DB] = new SocialLinkRepository[DB] {
    def findAllActive: DB[List[SocialLink]] = findAllActiveResult.pure[DB]
  }
}
