package su.wps.blog.endpoints.mocks

import cats.Applicative
import cats.syntax.applicative.*
import su.wps.blog.models.api.{AboutResult, ProfileResult}
import su.wps.blog.services.AboutService

object AboutServiceMock {
  def create[F[_]: Applicative](
    result: AboutResult = AboutResult(
      ProfileResult("", "", "", "", ""),
      Nil,
      Nil,
      Nil
    )
  ): AboutService[F] = new AboutService[F] {
    def getAboutPage: F[AboutResult] = result.pure[F]
  }
}
