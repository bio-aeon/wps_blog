package su.wps.blog.endpoints.mocks

import cats.Applicative
import cats.syntax.applicative.*
import su.wps.blog.models.api.FeedResult
import su.wps.blog.services.FeedService

object FeedServiceMock {
  def create[F[_]: Applicative](result: FeedResult = FeedResult(Nil, Nil, Nil)): FeedService[F] =
    new FeedService[F] {
      def getFeed: F[FeedResult] = result.pure[F]
    }
}
