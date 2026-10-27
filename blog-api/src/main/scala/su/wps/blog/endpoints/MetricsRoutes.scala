package su.wps.blog.endpoints

import cats.effect.Sync
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import io.prometheus.metrics.expositionformats.PrometheusTextFormatWriter
import io.prometheus.metrics.model.registry.PrometheusRegistry
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.`Content-Type`
import org.http4s.MediaType

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

object MetricsRoutes {

  def routes[F[_]](implicit F: Sync[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl.*

    HttpRoutes.of[F] { case GET -> Root / "metrics" =>
      F.delay {
        val out = new ByteArrayOutputStream()
        PrometheusTextFormatWriter.create().write(out, PrometheusRegistry.defaultRegistry.scrape())
        out.toString(StandardCharsets.UTF_8)
      }.flatMap(body => Ok(body, `Content-Type`(MediaType.text.plain)))
    }
  }
}
