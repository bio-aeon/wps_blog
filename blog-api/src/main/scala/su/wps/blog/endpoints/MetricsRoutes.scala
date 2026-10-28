package su.wps.blog.endpoints

import cats.MonadThrow
import fs2.compression.Compression
import org.http4s.HttpRoutes
import org.http4s.server.Router
import org.typelevel.otel4s.sdk.common.Diagnostic
import org.typelevel.otel4s.sdk.exporter.prometheus.{PrometheusHttpRoutes, PrometheusWriter}
import org.typelevel.otel4s.sdk.metrics.exporter.MetricExporter

private[blog] object MetricsRoutes {

  def routes[F[_]: MonadThrow: Compression: Diagnostic](
    exporter: MetricExporter.Pull[F]
  ): HttpRoutes[F] =
    Router("/metrics" -> PrometheusHttpRoutes.routes[F](exporter, PrometheusWriter.Config.default))
}
