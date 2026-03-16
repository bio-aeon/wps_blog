package su.wps.blog.endpoints

import cats.data.Kleisli
import cats.effect.Sync
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import com.github.benmanes.caffeine.cache.{Cache, Caffeine}
import io.circe.syntax.*
import org.http4s.*
import org.http4s.circe.*
import org.http4s.headers.`X-Forwarded-For`
import org.typelevel.ci.CIString
import su.wps.blog.models.api.ErrorResponse

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

object RateLimitMiddleware {

  private val RateLimitHeader = CIString("X-RateLimit-Limit")
  private val RemainingHeader = CIString("X-RateLimit-Remaining")
  private val RetryAfterHeader = CIString("Retry-After")

  def apply[F[_]: Sync](
    maxRequests: Int,
    windowSeconds: Long
  ): HttpApp[F] => HttpApp[F] = { app =>
    val cache: Cache[String, AtomicInteger] = Caffeine
      .newBuilder()
      .expireAfterWrite(windowSeconds, TimeUnit.SECONDS)
      .maximumSize(10000)
      .build()

    Kleisli { req =>
      if (req.method == Method.GET || req.method == Method.HEAD || req.method == Method.OPTIONS) {
        app.run(req)
      } else {
        Sync[F].delay {
          val ip = extractIp(req)
          val counter = cache.get(ip, _ => new AtomicInteger(0))
          counter.incrementAndGet()
        }.flatMap { count =>
          if (count > maxRequests) {
            Sync[F].pure(
              Response[F](Status.TooManyRequests)
                .withEntity(
                  ErrorResponse
                    .tooManyRequests("Rate limit exceeded. Please try again later.")
                    .asJson
                )
                .putHeaders(
                  Header.Raw(RateLimitHeader, maxRequests.toString),
                  Header.Raw(RemainingHeader, "0"),
                  Header.Raw(RetryAfterHeader, windowSeconds.toString)
                )
            )
          } else {
            app.run(req).map(
              _.putHeaders(
                Header.Raw(RateLimitHeader, maxRequests.toString),
                Header.Raw(RemainingHeader, (maxRequests - count).max(0).toString)
              )
            )
          }
        }
      }
    }
  }

  private def extractIp[F[_]](req: Request[F]): String =
    req.headers
      .get[`X-Forwarded-For`]
      .flatMap(_.values.head)
      .map(_.toUriString)
      .orElse(req.remoteAddr.map(_.toUriString))
      .getOrElse("unknown")
}
