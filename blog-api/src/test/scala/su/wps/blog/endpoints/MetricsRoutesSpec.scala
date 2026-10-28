package su.wps.blog.endpoints

import cats.effect.IO
import cats.effect.testing.specs2.CatsEffect
import org.http4s.*
import org.http4s.implicits.*
import org.http4s.otel4s.middleware.metrics.OtelMetrics
import org.specs2.mutable.Specification
import org.typelevel.otel4s.metrics.MeterProvider
import org.typelevel.otel4s.sdk.exporter.prometheus.PrometheusMetricExporter
import su.wps.blog.telemetry.Telemetry

class MetricsRoutesSpec extends Specification with CatsEffect {

  private val metricsRequest = Request[IO](Method.GET, uri"/metrics")

  "MetricsRoutes" >> {
    "GET /metrics" >> {
      "returns 200 OK" >> {
        for {
          exporter <- PrometheusMetricExporter.builder[IO].build
          resp <- MetricsRoutes.routes[IO](exporter).run(metricsRequest).value.map(_.get)
        } yield resp.status mustEqual Status.Ok
      }

      "returns Prometheus text format content type" >> {
        val prometheusTextFormat = MediaType.text.plain.withExtensions(Map("version" -> "0.0.4"))

        for {
          exporter <- PrometheusMetricExporter.builder[IO].build
          resp <- MetricsRoutes.routes[IO](exporter).run(metricsRequest).value.map(_.get)
        } yield resp.contentType.map(_.mediaType) must beSome(prometheusTextFormat)
      }

      "exposes count and latency series for served requests" >> {
        val app = HttpApp[IO](_ => IO.pure(Response[IO](Status.Ok)))
        val route = """classifier="/v1/posts/:id""""

        for {
          exporter <- PrometheusMetricExporter.builder[IO].build
          meterProvider <- Telemetry.meterProvider[IO](exporter.metricReader)
          metricsOps <- {
            implicit val mp: MeterProvider[IO] = meterProvider
            OtelMetrics.serverMetricsOps[IO]()
          }
          served <- MetricsMiddleware(metricsOps)(app).run(
            Request[IO](Method.GET, uri"/v1/posts/42")
          )
          _ <- served.body.compile.drain
          resp <- MetricsRoutes.routes[IO](exporter).run(metricsRequest).value.map(_.get)
          body <- resp.as[String]
        } yield {
          val series = body.linesIterator.toList
          series must contain(
            startWith("http_server_request_duration_seconds_count{") and contain(route) and
              contain("""http_response_status_code="200"""")
          )
          series must contain(
            startWith("http_server_request_duration_seconds_bucket{") and contain(route)
          )
        }
      }
    }
  }
}
