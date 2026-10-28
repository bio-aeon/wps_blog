package su.wps.blog.telemetry

import cats.effect.Async
import cats.effect.std.Random
import cats.mtl.Ask
import cats.syntax.flatMap.*
import org.typelevel.otel4s.Attributes
import org.typelevel.otel4s.metrics.MeterProvider
import org.typelevel.otel4s.sdk.TelemetryResource
import org.typelevel.otel4s.sdk.common.Diagnostic
import org.typelevel.otel4s.sdk.context.{AskContext, Context}
import org.typelevel.otel4s.sdk.metrics.SdkMeterProvider
import org.typelevel.otel4s.sdk.metrics.exporter.MetricReader
import org.typelevel.otel4s.semconv.attributes.ServiceAttributes

private[blog] object Telemetry {

  private val ServiceName = "blog-api"

  def meterProvider[F[_]: Async: Diagnostic](reader: MetricReader[F]): F[MeterProvider[F]] =
    Random.scalaUtilRandom[F].flatMap { implicit random =>
      // The service records metrics only, so there is no trace context for exemplars to link to.
      implicit val askContext: AskContext[F] = Ask.const[F, Context](Context.root)
      SdkMeterProvider
        .builder[F]
        .addResource(TelemetryResource(Attributes(ServiceAttributes.ServiceName(ServiceName))))
        .registerMetricReader(reader)
        .build
    }
}
