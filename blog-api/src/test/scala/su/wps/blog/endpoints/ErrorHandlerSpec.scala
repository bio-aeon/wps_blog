package su.wps.blog.endpoints

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.http4s.*
import org.http4s.dsl.io.*
import org.http4s.implicits.*
import org.specs2.mutable.Specification
import su.wps.blog.models.domain.{AppErr, CommentId, PostId}

class ErrorHandlerSpec extends Specification {

  "ErrorHandler" >> {
    "return 404 for PostNotFound error" >> {
      val routes = mkFailingRoutes(AppErr.PostNotFound(PostId(123)))
      val request = Request[IO](Method.GET, uri"/test")

      val resp = ErrorHandler(routes).run(request).value.map(_.get).unsafeRunSync()

      resp.status mustEqual Status.NotFound
    }

    "include post id in NotFound response body for PostNotFound" >> {
      val routes = mkFailingRoutes(AppErr.PostNotFound(PostId(456)))
      val request = Request[IO](Method.GET, uri"/test")

      val resp = ErrorHandler(routes).run(request).value.map(_.get).unsafeRunSync()
      val body = resp.as[String].unsafeRunSync()

      body must contain("\"code\":\"NOT_FOUND\"")
      body must contain("Post not found: 456")
    }

    "return 404 for CommentNotFound error" >> {
      val routes = mkFailingRoutes(AppErr.CommentNotFound(CommentId(789)))
      val request = Request[IO](Method.GET, uri"/test")

      val resp = ErrorHandler(routes).run(request).value.map(_.get).unsafeRunSync()

      resp.status mustEqual Status.NotFound
    }

    "include comment id in NotFound response body for CommentNotFound" >> {
      val routes = mkFailingRoutes(AppErr.CommentNotFound(CommentId(999)))
      val request = Request[IO](Method.GET, uri"/test")

      val resp = ErrorHandler(routes).run(request).value.map(_.get).unsafeRunSync()
      val body = resp.as[String].unsafeRunSync()

      body must contain("\"code\":\"NOT_FOUND\"")
      body must contain("Comment not found: 999")
    }

    "return 404 for PageNotFound error" >> {
      val routes = mkFailingRoutes(AppErr.PageNotFound("about-us"))
      val request = Request[IO](Method.GET, uri"/test")

      val resp = ErrorHandler(routes).run(request).value.map(_.get).unsafeRunSync()

      resp.status mustEqual Status.NotFound
    }

    "include page url in NotFound response body for PageNotFound" >> {
      val routes = mkFailingRoutes(AppErr.PageNotFound("contact"))
      val request = Request[IO](Method.GET, uri"/test")

      val resp = ErrorHandler(routes).run(request).value.map(_.get).unsafeRunSync()
      val body = resp.as[String].unsafeRunSync()

      body must contain("\"code\":\"NOT_FOUND\"")
      body must contain("Page not found: contact")
    }

    "return 500 for unexpected exceptions" >> {
      val routes = mkFailingRoutes(new RuntimeException("Unexpected error"))
      val request = Request[IO](Method.GET, uri"/test")

      val resp = ErrorHandler(routes).run(request).value.map(_.get).unsafeRunSync()

      resp.status mustEqual Status.InternalServerError
    }

    "hide internal error details in response body" >> {
      val routes = mkFailingRoutes(new RuntimeException("Database connection failed"))
      val request = Request[IO](Method.GET, uri"/test")

      val resp = ErrorHandler(routes).run(request).value.map(_.get).unsafeRunSync()
      val body = resp.as[String].unsafeRunSync()

      body must contain("\"code\":\"INTERNAL_ERROR\"")
      body must contain("An unexpected error occurred")
      body must not contain "Database connection failed"
    }

    "pass through successful responses unchanged" >> {
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
