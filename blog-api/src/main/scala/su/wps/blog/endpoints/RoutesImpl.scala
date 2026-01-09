package su.wps.blog.endpoints

import cats.Monad
import cats.syntax.apply.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import io.circe.syntax.*
import org.http4s.HttpRoutes
import org.http4s.circe.*
import org.http4s.dsl.Http4sDsl
import su.wps.blog.models.domain.PostId
import su.wps.blog.services.{CommentService, PostService}

final class RoutesImpl[F[_]: Monad] private (
  postService: PostService[F],
  commentService: CommentService[F]
) extends Http4sDsl[F]
    with Routes[F] {
  import RoutesImpl._

  private object LimitParamMatcher extends QueryParamDecoderMatcher[Int]("limit")
  private object OffsetParamMatcher extends QueryParamDecoderMatcher[Int]("offset")
  private object TagParamMatcher extends OptionalQueryParamDecoderMatcher[String]("tag")
  private object QueryParamMatcher extends QueryParamDecoderMatcher[String]("q")
  private object CountParamMatcher extends OptionalQueryParamDecoderMatcher[Int]("count")

  val routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "posts" :? LimitParamMatcher(limit) +& OffsetParamMatcher(offset)
        +& TagParamMatcher(maybeTag) =>
      maybeTag match {
        case Some(tagSlug) =>
          postService.postsByTag(tagSlug, limit, offset).map(_.asJson).flatMap(Ok(_))
        case None =>
          postService.allPosts(limit, offset).map(_.asJson).flatMap(Ok(_))
      }

    case GET -> Root / "posts" / "search" :? QueryParamMatcher(query) +& LimitParamMatcher(limit)
        +& OffsetParamMatcher(offset) =>
      postService.searchPosts(query, limit, offset).map(_.asJson).flatMap(Ok(_))

    case GET -> Root / "posts" / "recent" :? CountParamMatcher(maybeCount) =>
      val count = maybeCount
        .getOrElse(DefaultRecentPostsCount)
        .min(MaxRecentPostsCount)
        .max(MinRecentPostsCount)
      postService.recentPosts(count).map(_.asJson).flatMap(Ok(_))

    case GET -> Root / "posts" / IntVar(id) =>
      postService.postById(PostId(id)).map(_.asJson).flatMap(Ok(_))

    case POST -> Root / "posts" / IntVar(id) / "view" =>
      postService.incrementViewCount(PostId(id)) *> NoContent()

    case GET -> Root / "posts" / IntVar(id) / "comments" =>
      commentService.getCommentsForPost(PostId(id)).map(_.asJson).flatMap(Ok(_))
  }
}

object RoutesImpl {
  val DefaultRecentPostsCount = 5
  val MaxRecentPostsCount = 20
  val MinRecentPostsCount = 1

  def create[F[_]: Monad](
    postService: PostService[F],
    commentService: CommentService[F]
  ): RoutesImpl[F] =
    new RoutesImpl[F](postService, commentService)
}
