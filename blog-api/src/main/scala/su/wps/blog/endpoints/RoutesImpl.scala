package su.wps.blog.endpoints

import cats.effect.Concurrent
import cats.syntax.apply.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import io.circe.syntax.*
import org.http4s.{HttpRoutes, Request}
import org.http4s.circe.*
import org.http4s.circe.CirceEntityDecoder.*
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.`X-Forwarded-For`
import su.wps.blog.models.api.{CreateCommentRequest, RateCommentRequest}
import su.wps.blog.models.domain.CommentId
import su.wps.blog.models.domain.PostId
import su.wps.blog.services.{CommentService, PageService, PostService, TagService}

final class RoutesImpl[F[_]: Concurrent] private (
  postService: PostService[F],
  commentService: CommentService[F],
  tagService: TagService[F],
  pageService: PageService[F]
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

    case req @ POST -> Root / "posts" / IntVar(id) / "comments" =>
      req.as[CreateCommentRequest].flatMap { request =>
        commentService.createComment(PostId(id), request).map(_.asJson).flatMap(Created(_))
      }

    case req @ POST -> Root / "comments" / IntVar(id) / "rate" =>
      val ip = extractIp(req)
      req.as[RateCommentRequest].flatMap { request =>
        commentService.rateComment(CommentId(id), request.isUpvote, ip) *> NoContent()
      }

    case DELETE -> Root / "admin" / "comments" / IntVar(id) =>
      commentService.deleteComment(CommentId(id)) *> NoContent()

    case PUT -> Root / "admin" / "comments" / IntVar(id) / "approve" =>
      commentService.approveComment(CommentId(id)) *> NoContent()

    case GET -> Root / "tags" / "cloud" =>
      tagService.getTagCloud.map(_.asJson).flatMap(Ok(_))

    case GET -> Root / "tags" =>
      tagService.getAllTags.map(_.asJson).flatMap(Ok(_))

    case GET -> Root / "pages" =>
      pageService.getAllPages.map(_.asJson).flatMap(Ok(_))

    case GET -> Root / "pages" / url =>
      pageService.getPageByUrl(url).map(_.asJson).flatMap(Ok(_))
  }

  private def extractIp(req: Request[F]): String =
    req.headers
      .get[`X-Forwarded-For`]
      .flatMap(_.values.head)
      .map(_.toUriString)
      .orElse(req.remoteAddr.map(_.toUriString))
      .getOrElse("unknown")
}

object RoutesImpl {
  val DefaultRecentPostsCount = 5
  val MaxRecentPostsCount = 20
  val MinRecentPostsCount = 1

  def create[F[_]: Concurrent](
    postService: PostService[F],
    commentService: CommentService[F],
    tagService: TagService[F],
    pageService: PageService[F]
  ): RoutesImpl[F] =
    new RoutesImpl[F](postService, commentService, tagService, pageService)
}
