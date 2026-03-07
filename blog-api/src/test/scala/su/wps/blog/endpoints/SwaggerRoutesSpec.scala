package su.wps.blog.endpoints

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.http4s.*
import org.http4s.implicits.*
import org.specs2.mutable.Specification

class SwaggerRoutesSpec extends Specification {

  private val swaggerRoutes = SwaggerRoutes.routes[IO]

  "SwaggerRoutes" >> {
    "serves Swagger UI at /docs" >> {
      val request = Request[IO](Method.GET, uri"/docs")
      val resp = swaggerRoutes.run(request).value.unsafeRunSync()

      resp must beSome
      resp.get.status mustEqual Status.PermanentRedirect
    }

    "serves OpenAPI YAML spec at /docs/docs.yaml" >> {
      val request = Request[IO](Method.GET, uri"/docs/docs.yaml")
      val resp = swaggerRoutes.run(request).value.unsafeRunSync()

      resp must beSome
      resp.get.status mustEqual Status.Ok
    }

    "includes API title in OpenAPI spec" >> {
      val request = Request[IO](Method.GET, uri"/docs/docs.yaml")
      val resp = swaggerRoutes.run(request).value.map(_.get).unsafeRunSync()
      val body = resp.as[String].unsafeRunSync()

      body must contain("WPS Blog API")
    }

    "includes API version in OpenAPI spec" >> {
      val request = Request[IO](Method.GET, uri"/docs/docs.yaml")
      val resp = swaggerRoutes.run(request).value.map(_.get).unsafeRunSync()
      val body = resp.as[String].unsafeRunSync()

      body must contain("1.0.0")
    }

    "includes posts endpoints in OpenAPI spec" >> {
      val request = Request[IO](Method.GET, uri"/docs/docs.yaml")
      val resp = swaggerRoutes.run(request).value.map(_.get).unsafeRunSync()
      val body = resp.as[String].unsafeRunSync()

      body must contain("/posts")
      body must contain("/posts/search")
      body must contain("/posts/recent")
    }

    "includes comments endpoints in OpenAPI spec" >> {
      val request = Request[IO](Method.GET, uri"/docs/docs.yaml")
      val resp = swaggerRoutes.run(request).value.map(_.get).unsafeRunSync()
      val body = resp.as[String].unsafeRunSync()

      body must contain("/comments")
    }

    "includes tags endpoints in OpenAPI spec" >> {
      val request = Request[IO](Method.GET, uri"/docs/docs.yaml")
      val resp = swaggerRoutes.run(request).value.map(_.get).unsafeRunSync()
      val body = resp.as[String].unsafeRunSync()

      body must contain("/tags")
      body must contain("/tags/cloud")
    }

    "includes pages endpoints in OpenAPI spec" >> {
      val request = Request[IO](Method.GET, uri"/docs/docs.yaml")
      val resp = swaggerRoutes.run(request).value.map(_.get).unsafeRunSync()
      val body = resp.as[String].unsafeRunSync()

      body must contain("/pages")
    }

    "includes health endpoint in OpenAPI spec" >> {
      val request = Request[IO](Method.GET, uri"/docs/docs.yaml")
      val resp = swaggerRoutes.run(request).value.map(_.get).unsafeRunSync()
      val body = resp.as[String].unsafeRunSync()

      body must contain("/health")
    }
  }

  "ApiEndpoints" >> {
    "defines all 19 endpoints" >> {
      ApiEndpoints.all must have size 19
    }
  }
}
