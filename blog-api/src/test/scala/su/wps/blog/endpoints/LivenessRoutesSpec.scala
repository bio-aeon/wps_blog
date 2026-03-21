package su.wps.blog.endpoints

import cats.effect.IO
import cats.effect.testing.specs2.CatsEffect
import org.http4s.*
import org.http4s.implicits.*
import org.specs2.mutable.Specification

class LivenessRoutesSpec extends Specification with CatsEffect {

  "LivenessRoutes" >> {
    "GET /health/live" >> {
      "returns 200 OK" >> {
        val request = Request[IO](Method.GET, uri"/health/live")

        LivenessRoutes.routes[IO].run(request).value.map(_.get).map { resp =>
          resp.status mustEqual Status.Ok
        }
      }
    }
  }
}
