package su.wps.blog.endpoints

import cats.data.Kleisli
import cats.effect.Sync
import cats.syntax.functor.*
import org.http4s.*
import org.typelevel.ci.CIString

import java.util.UUID

object CorrelationIdMiddleware {

  private val RequestIdHeader = CIString("X-Request-Id")

  def apply[F[_]: Sync](app: HttpApp[F]): HttpApp[F] =
    Kleisli { req =>
      val requestId = req.headers
        .get(RequestIdHeader)
        .map(_.head.value)
        .getOrElse(UUID.randomUUID().toString)

      app
        .run(req.putHeaders(Header.Raw(RequestIdHeader, requestId)))
        .map(_.putHeaders(Header.Raw(RequestIdHeader, requestId)))
    }
}
