package su.wps.blog.endpoints.mocks

import cats.Applicative
import cats.syntax.applicative.*
import su.wps.blog.models.api.{ListItemsResult, ListPostResult, PostResult}
import su.wps.blog.models.domain.AppErr.PostNotFound
import su.wps.blog.models.domain.{AppErr, PostId}
import su.wps.blog.services.PostService
import tofu.Raise

object PostServiceMock {
  def create[F[_]: Applicative](
    allPostsResult: List[ListPostResult] = Nil,
    postByIdResult: Option[PostResult] = None,
    postsByTagResult: List[ListPostResult] = Nil,
    searchPostsResult: List[ListPostResult] = Nil,
    incrementViewCountSuccess: Boolean = true
  )(implicit R: Raise[F, AppErr]): PostService[F] =
    new PostService[F] {
      def allPosts(limit: Int, offset: Int): F[ListItemsResult[ListPostResult]] =
        ListItemsResult(allPostsResult, allPostsResult.length).pure[F]

      def postsByTag(tagSlug: String, limit: Int, offset: Int): F[ListItemsResult[ListPostResult]] =
        ListItemsResult(postsByTagResult, postsByTagResult.length).pure[F]

      def postById(id: PostId): F[PostResult] =
        postByIdResult.map(_.pure[F]).getOrElse(R.raise(PostNotFound(id)))

      def incrementViewCount(id: PostId): F[Unit] =
        ().pure[F]

      def searchPosts(query: String, limit: Int, offset: Int): F[ListItemsResult[ListPostResult]] =
        ListItemsResult(searchPostsResult, searchPostsResult.length).pure[F]
    }
}
