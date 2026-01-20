package su.wps.blog.endpoints.mocks

import cats.Applicative
import cats.syntax.applicative.*
import su.wps.blog.models.api.PageResult
import su.wps.blog.models.domain.AppErr
import su.wps.blog.models.domain.AppErr.PageNotFound
import su.wps.blog.services.PageService
import tofu.Raise

object PageServiceMock {
  def create[F[_]: Applicative](
    getPageByUrlResult: Option[PageResult] = None
  )(implicit R: Raise[F, AppErr]): PageService[F] = new PageService[F] {
    def getPageByUrl(url: String): F[PageResult] =
      getPageByUrlResult.map(_.pure[F]).getOrElse(R.raise(PageNotFound(url)))
  }
}
