package su.wps.blog.endpoints

import cats.data.{Kleisli, OptionT}
import cats.effect.Sync
import cats.syntax.applicativeError.*
import cats.syntax.functor.*
import io.circe.syntax.*
import org.http4s.*
import org.http4s.circe.*
import org.http4s.dsl.Http4sDsl
import su.wps.blog.models.api.ErrorResponse
import su.wps.blog.models.domain.AppErr

object ErrorHandler {

  def apply[F[_]: Sync](routes: HttpRoutes[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl.*

    Kleisli { request =>
      routes(request).handleErrorWith { error =>
        OptionT.liftF(toResponse[F](error))
      }
    }
  }

  def httpApp[F[_]: Sync](app: HttpApp[F]): HttpApp[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl.*

    Kleisli { request =>
      app(request).handleErrorWith { error =>
        toResponse[F](error)
      }
    }
  }

  private def toResponse[F[_]: Sync](error: Throwable): F[Response[F]] = {
    val dsl = new Http4sDsl[F] {}
    import dsl.*

    error match {
      case AppErr.PostNotFound(id) =>
        NotFound(ErrorResponse.notFound("Post", id.value.toString).asJson)

      case AppErr.CommentNotFound(id) =>
        NotFound(ErrorResponse.notFound("Comment", id.value.toString).asJson)

      case AppErr.PageNotFound(url) =>
        NotFound(ErrorResponse.notFound("Page", url).asJson)

      case e: InvalidMessageBodyFailure =>
        BadRequest(ErrorResponse.badRequest(e.getMessage).asJson)

      case e: MalformedMessageBodyFailure =>
        BadRequest(ErrorResponse.badRequest(e.getMessage).asJson)

      case _: Throwable =>
        InternalServerError(ErrorResponse.internal("An unexpected error occurred").asJson)
    }
  }
}
