package su.wps.blog.endpoints.mocks

import cats.Applicative
import cats.syntax.applicative.*
import su.wps.blog.models.api.SocialLinkResult
import su.wps.blog.services.SocialLinkService

object SocialLinkServiceMock {
  def create[F[_]: Applicative](
    result: List[SocialLinkResult] = Nil
  ): SocialLinkService[F] = new SocialLinkService[F] {
    def getSocialLinks: F[List[SocialLinkResult]] = result.pure[F]
  }
}
