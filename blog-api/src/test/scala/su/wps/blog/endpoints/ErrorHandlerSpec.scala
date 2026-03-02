package su.wps.blog.endpoints

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.http4s.*
import org.http4s.dsl.io.*
import org.http4s.implicits.*
import org.specs2.mutable.Specification
import su.wps.blog.models.domain.{AppErr, PostId}

class ErrorHandlerSpec extends Specification {

  "ErrorHandler" >> {
    "returns 404 for PostNotFound error" >> {
      val routes = mkFailingRoutes(AppErr.PostNotFound(PostId(123)))
      val request = Request[IO](Method.GET, uri"/test")

      val resp = ErrorHandler(routes).run(request).value.map(_.get).unsafeRunSync()

      resp.status mustEqual Status.NotFound
    }

    "includes post id in NotFound response body for PostNotFound" >> {
      val routes = mkFailingRoutes(AppErr.PostNotFound(PostId(456)))
      val request = Request[IO](Method.GET, uri"/test")

      val resp = ErrorHandler(routes).run(request).value.map(_.get).unsafeRunSync()
      val body = resp.as[String].unsafeRunSync()

      body must contain("\"code\":\"NOT_FOUND\"")
      body must contain("Post not found: 456")
    }

    "returns 404 for PageNotFound error" >> {
      val routes = mkFailingRoutes(AppErr.PageNotFound("about-us"))
      val request = Request[IO](Method.GET, uri"/test")

      val resp = ErrorHandler(routes).run(request).value.map(_.get).unsafeRunSync()

      resp.status mustEqual Status.NotFound
    }

    "includes page url in NotFound response body for PageNotFound" >> {
      val routes = mkFailingRoutes(AppErr.PageNotFound("contact"))
      val request = Request[IO](Method.GET, uri"/test")

      val resp = ErrorHandler(routes).run(request).value.map(_.get).unsafeRunSync()
      val body = resp.as[String].unsafeRunSync()

      body must contain("\"code\":\"NOT_FOUND\"")
      body must contain("Page not found: contact")
    }

    "returns 500 for unexpected exceptions" >> {
      val routes = mkFailingRoutes(new RuntimeException("Unexpected error"))
      val request = Request[IO](Method.GET, uri"/test")

      val resp = ErrorHandler(routes).run(request).value.map(_.get).unsafeRunSync()

      resp.status mustEqual Status.InternalServerError
    }

    "hides internal error details in response body" >> {
      val routes = mkFailingRoutes(new RuntimeException("Database connection failed"))
      val request = Request[IO](Method.GET, uri"/test")

      val resp = ErrorHandler(routes).run(request).value.map(_.get).unsafeRunSync()
      val body = resp.as[String].unsafeRunSync()

      body must contain("\"code\":\"INTERNAL_ERROR\"")
      body must contain("An unexpected error occurred")
      body must not contain "Database connection failed"
    }

    "returns 400 for ValidationFailed error" >> {
      val errors = Map("limit" -> "Must be between 1 and 100", "offset" -> "Must be non-negative")
      val routes = mkFailingRoutes(AppErr.ValidationFailed(errors))
      val request = Request[IO](Method.GET, uri"/test")

      val resp = ErrorHandler(routes).run(request).value.map(_.get).unsafeRunSync()

      resp.status mustEqual Status.BadRequest
    }

    "includes validation error details in response body" >> {
      val errors = Map("limit" -> "Must be between 1 and 100")
      val routes = mkFailingRoutes(AppErr.ValidationFailed(errors))
      val request = Request[IO](Method.GET, uri"/test")

      val resp = ErrorHandler(routes).run(request).value.map(_.get).unsafeRunSync()
      val body = resp.as[String].unsafeRunSync()

      body must contain("\"code\":\"VALIDATION_ERROR\"")
      body must contain("\"limit\"")
      body must contain("Must be between 1 and 100")
    }

    "passes through successful responses unchanged" >> {
      val routes = HttpRoutes.of[IO] { case GET -> Root / "test" =>
        IO.pure(Response[IO](Status.Ok).withEntity("success"))
      }
      val request = Request[IO](Method.GET, uri"/test")

      val resp = ErrorHandler(routes).run(request).value.map(_.get).unsafeRunSync()

      resp.status mustEqual Status.Ok
      resp.as[String].unsafeRunSync() mustEqual "success"
    }
  }

  private def mkFailingRoutes(error: Throwable): HttpRoutes[IO] =
    HttpRoutes.of[IO] { case GET -> Root / "test" =>
      IO.raiseError(error)
    }
}
