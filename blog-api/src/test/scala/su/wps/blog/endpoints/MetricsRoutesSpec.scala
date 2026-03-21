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
    }
  }
}
