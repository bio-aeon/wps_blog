package su.wps.blog.endpoints

import cats.Functor
import cats.data.Kleisli
import cats.syntax.functor.*
import org.http4s.*
import org.typelevel.ci.CIString

object SecurityHeadersMiddleware {

  private val headers = List(
    Header.Raw(CIString("X-Content-Type-Options"), "nosniff"),
    Header.Raw(CIString("X-Frame-Options"), "DENY"),
    Header.Raw(CIString("Referrer-Policy"), "strict-origin-when-cross-origin"),
    Header.Raw(CIString("Permissions-Policy"), "camera=(), microphone=(), geolocation=()"),
    Header.Raw(
      CIString("Content-Security-Policy"),
      "default-src 'none'; frame-ancestors 'none'"
    ),
    Header.Raw(
      CIString("Strict-Transport-Security"),
      "max-age=31536000; includeSubDomains"
    )
  )

  def apply[F[_]: Functor](app: HttpApp[F]): HttpApp[F] =
    Kleisli { req =>
      app.run(req).map { response =>
        headers.foldLeft(response) { (resp, header) =>
          resp.putHeaders(header)
        }
      }
    }
}
