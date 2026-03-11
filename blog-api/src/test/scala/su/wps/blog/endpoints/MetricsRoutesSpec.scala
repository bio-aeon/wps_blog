package su.wps.blog.endpoints

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.http4s.*
import org.http4s.implicits.*
import org.specs2.mutable.Specification

class MetricsRoutesSpec extends Specification {

  "MetricsRoutes" >> {
    "GET /metrics" >> {
      "returns 200 OK" >> {
        val request = Request[IO](Method.GET, uri"/metrics")

        val resp = MetricsRoutes.routes[IO].run(request).value.map(_.get).unsafeRunSync()

        resp.status mustEqual Status.Ok
      }

      "returns text/plain content type" >> {
        val request = Request[IO](Method.GET, uri"/metrics")

        val resp = MetricsRoutes.routes[IO].run(request).value.map(_.get).unsafeRunSync()

        resp.contentType.map(_.mediaType) must beSome(MediaType.text.plain)
      }
    }
  }
}
