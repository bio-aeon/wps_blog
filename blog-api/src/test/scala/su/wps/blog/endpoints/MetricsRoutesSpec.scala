package su.wps.blog.endpoints

import cats.effect.IO
import cats.effect.testing.specs2.CatsEffect
import org.http4s.*
import org.http4s.implicits.*
import org.specs2.mutable.Specification

class MetricsRoutesSpec extends Specification with CatsEffect {

  "MetricsRoutes" >> {
    "GET /metrics" >> {
      "returns 200 OK" >> {
        val request = Request[IO](Method.GET, uri"/metrics")

        MetricsRoutes.routes[IO].run(request).value.map(_.get).map { resp =>
          resp.status mustEqual Status.Ok
        }
      }

      "returns text/plain content type" >> {
        val request = Request[IO](Method.GET, uri"/metrics")

        MetricsRoutes.routes[IO].run(request).value.map(_.get).map { resp =>
          resp.contentType.map(_.mediaType) must beSome(MediaType.text.plain)
        }
      }

      "exposes count and latency series for served requests" >> {
        val app = MetricsMiddleware(HttpApp[IO](_ => IO.pure(Response[IO](Status.Ok))))
        val labels = """method="GET",path="/v1/posts/:id",status="200""""
        val request = Request[IO](Method.GET, uri"/metrics")

        for {
          _ <- app.run(Request[IO](Method.GET, uri"/v1/posts/42"))
          resp <- MetricsRoutes.routes[IO].run(request).value.map(_.get)
          body <- resp.as[String]
        } yield {
          body must contain(s"http_requests_total{$labels} ")
          body must contain(s"""http_request_duration_seconds_bucket{$labels,le="0.005"} """)
        }
      }
    }
  }
}
