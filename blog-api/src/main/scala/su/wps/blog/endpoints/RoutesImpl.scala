package su.wps.blog.endpoints

import cats.Monad
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.circe.syntax._
import org.http4s.HttpRoutes
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import su.wps.blog.models.domain.PostId
import su.wps.blog.services.PostService

final class RoutesImpl[F[_]: Monad] private (postService: PostService[F])
    extends Http4sDsl[F]
    with Routes[F] {

  private object LimitParamMatcher extends QueryParamDecoderMatcher[Int]("limit")
  private object OffsetParamMatcher extends QueryParamDecoderMatcher[Int]("offset")

  val routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "posts" :? LimitParamMatcher(limit) +& OffsetParamMatcher(offset) =>
      postService.allPosts(limit, offset).map(_.asJson).flatMap(Ok(_))

    case GET -> Root / "posts" / IntVar(id) =>
      postService.postById(PostId(id)).map(_.asJson).flatMap(Ok(_))
  }
}

object RoutesImpl {

  def create[F[_]: Monad](postService: PostService[F]): RoutesImpl[F] =
    new RoutesImpl[F](postService)
}
