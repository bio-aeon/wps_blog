package su.wps.blog.endpoints

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.http4s.*
import org.http4s.implicits.*
import org.specs2.mutable.Specification

class SystemRoutesSpec extends Specification with RoutesSpecSupport {

  "GET /health" >> {
    "returns health status" >> {
      val routes = buildRoutes[IO]()
      val request = Request[IO](Method.GET, uri"health")

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()

      resp.status mustEqual Status.Ok
    }

    "returns status, database and timestamp fields" >> {
      val routes = buildRoutes[IO]()
      val request = Request[IO](Method.GET, uri"health")

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      val respBody = resp.as[String].unsafeRunSync()

      respBody must contain("\"status\":\"healthy\"")
      respBody must contain("\"database\":\"healthy\"")
      respBody must contain("\"timestamp\":")
    }

    "returns degraded status when database unhealthy" >> {
      val routes = buildRoutes[IO](healthStatus = "degraded", healthDatabase = "unhealthy")
      val request = Request[IO](Method.GET, uri"health")

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      val respBody = resp.as[String].unsafeRunSync()

      resp.status mustEqual Status.Ok
      respBody must contain("\"status\":\"degraded\"")
      respBody must contain("\"database\":\"unhealthy\"")
    }
  }

  "routing" >> {
    "serves API routes under /v1 prefix" >> {
      val routes = buildRoutes[IO]()
      val request = Request[IO](
        Method.GET,
        Uri.unsafeFromString(s"$v1/posts").withQueryParams(Map("limit" -> "10", "offset" -> "0"))
      )

      val resp = routes.routes.run(request).value.unsafeRunSync()

      resp must beSome
      resp.get.status mustEqual Status.Ok
    }

    "rejects routes without version prefix" >> {
      val routes = buildRoutes[IO]()
      val request =
        Request[IO](Method.GET, uri"posts".withQueryParams(Map("limit" -> "10", "offset" -> "0")))

      val resp = routes.routes.run(request).value.unsafeRunSync()

      resp must beNone
    }

    "serves health without version prefix" >> {
      val routes = buildRoutes[IO]()
      val request = Request[IO](Method.GET, uri"health")

      val resp = routes.routes.run(request).value.unsafeRunSync()

      resp must beSome
      resp.get.status mustEqual Status.Ok
    }
  }

}
