package su.wps.blog.endpoints

import cats.data.Kleisli
import cats.effect.{Clock, Sync}
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import io.prometheus.client.{CollectorRegistry, Counter, Histogram}
import org.http4s.*

object MetricsMiddleware {

  private val RequestDuration: Histogram = Histogram
    .build()
    .name("http_request_duration_seconds")
    .help("HTTP request latency in seconds")
    .labelNames("method", "path", "status")
    .buckets(0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1.0, 2.5)
    .register()

  private val RequestTotal: Counter = Counter
    .build()
    .name("http_requests_total")
    .help("Total HTTP requests")
    .labelNames("method", "path", "status")
    .register()

  def apply[F[_]: Sync: Clock](app: HttpApp[F]): HttpApp[F] =
    Kleisli { req =>
      for {
        start <- Clock[F].monotonic
        response <- app.run(req)
        end <- Clock[F].monotonic
        elapsed = (end - start).toUnit(java.util.concurrent.TimeUnit.SECONDS)
        method = req.method.name
        path = normalizePath(req.uri.path.renderString)
        status = response.status.code.toString
        _ <- Sync[F].delay {
          RequestDuration.labels(method, path, status).observe(elapsed)
          RequestTotal.labels(method, path, status).inc()
        }
      } yield response
    }

  private def normalizePath(path: String): String = path match {
    case p if p.matches(".*/posts/\\d+/comments$") => "/v1/posts/:id/comments"
    case p if p.matches(".*/posts/\\d+/view$") => "/v1/posts/:id/view"
    case p if p.matches(".*/posts/\\d+$") => "/v1/posts/:id"
    case p if p.matches(".*/comments/\\d+/rate$") => "/v1/comments/:id/rate"
    case p if p.matches(".*/pages/.+$") => "/v1/pages/:url"
    case other => other
  }
}
