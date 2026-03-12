package su.wps.blog.endpoints

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.http4s.*
import org.http4s.implicits.*
import org.specs2.mutable.Specification

class LivenessRoutesSpec extends Specification {

  "LivenessRoutes" >> {
    "GET /health/live" >> {
      "returns 200 OK" >> {
        val request = Request[IO](Method.GET, uri"/health/live")

        val resp =
          LivenessRoutes.routes[IO].run(request).value.map(_.get).unsafeRunSync()

        resp.status mustEqual Status.Ok
      }
    }
  }
}
