package su.wps.blog.endpoints

import cats.effect.Sync
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.`Content-Type`
import org.http4s.MediaType

import java.io.StringWriter

object MetricsRoutes {

  def routes[F[_]](implicit F: Sync[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl.*

    HttpRoutes.of[F] { case GET -> Root / "metrics" =>
      F.delay {
        val writer = new StringWriter()
        TextFormat.write004(writer, CollectorRegistry.defaultRegistry.metricFamilySamples())
        writer.toString
      }.flatMap(body =>
        Ok(body, `Content-Type`(MediaType.text.plain))
      )
    }
  }
}
