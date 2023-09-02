package su.wps.blog.endpoints.mocks

import cats.Applicative
import cats.syntax.applicative._
import su.wps.blog.models.api.{ListItemsResult, ListPostResult, PostResult}
import su.wps.blog.models.domain.AppErr.PostNotFound
import su.wps.blog.models.domain.{AppErr, PostId}
import su.wps.blog.services.PostService
import tofu.Raise

object PostServiceMock {
  def create[F[_]: Applicative](
    allPostsResult: List[ListPostResult] = Nil,
    postByIdResult: Option[PostResult] = None
  )(implicit R: Raise[F, AppErr]): PostService[F] =
    new PostService[F] {
      def allPosts(limit: Int, offset: Int): F[ListItemsResult[ListPostResult]] =
        ListItemsResult(allPostsResult, allPostsResult.length).pure[F]

      def postById(id: PostId): F[PostResult] =
        postByIdResult.map(_.pure[F]).getOrElse(R.raise(PostNotFound(id)))
    }
}
