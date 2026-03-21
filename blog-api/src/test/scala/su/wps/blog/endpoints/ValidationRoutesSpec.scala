package su.wps.blog.endpoints

import cats.effect.IO
import cats.effect.testing.specs2.CatsEffect
import io.circe.syntax.*
import org.http4s.*
import org.http4s.circe.*
import org.http4s.implicits.*
import org.specs2.mutable.Specification
import su.wps.blog.models.api.*

class ValidationRoutesSpec extends Specification with RoutesSpecSupport with CatsEffect {

  "validation" >> {
    "GET /posts" >> {
      "rejects invalid pagination limit" >> {
        val routes = buildRoutes[IO]()
        val request = Request[IO](
          Method.GET,
          Uri.unsafeFromString(s"$v1/posts").withQueryParams(Map("limit" -> "0", "offset" -> "0"))
        )

        for {
          resp <- ErrorHandler(routes.routes).run(request).value.map(_.get)
          body <- resp.as[String]
        } yield {
          resp.status mustEqual Status.BadRequest
          body must contain("\"code\":\"VALIDATION_ERROR\"")
          body must contain("\"limit\"")
        }
      }

      "rejects negative pagination offset" >> {
        val routes = buildRoutes[IO]()
        val request = Request[IO](
          Method.GET,
          Uri.unsafeFromString(s"$v1/posts").withQueryParams(Map("limit" -> "10", "offset" -> "-1"))
        )

        for {
          resp <- ErrorHandler(routes.routes).run(request).value.map(_.get)
          body <- resp.as[String]
        } yield {
          resp.status mustEqual Status.BadRequest
          body must contain("\"code\":\"VALIDATION_ERROR\"")
          body must contain("\"offset\"")
        }
      }

      "rejects limit exceeding maximum" >> {
        val routes = buildRoutes[IO]()
        val request = Request[IO](
          Method.GET,
          Uri.unsafeFromString(s"$v1/posts").withQueryParams(Map("limit" -> "101", "offset" -> "0"))
        )

        ErrorHandler(routes.routes).run(request).value.map(_.get).map { resp =>
          resp.status mustEqual Status.BadRequest
        }
      }
    }

    "GET /posts/search" >> {
      "rejects invalid pagination" >> {
        val routes = buildRoutes[IO](searchPostsResult = testSearchResults)
        val request = Request[IO](
          Method.GET,
          Uri
            .unsafeFromString(s"$v1/posts/search")
            .withQueryParams(Map("q" -> "scala", "limit" -> "-1", "offset" -> "0"))
        )

        ErrorHandler(routes.routes).run(request).value.map(_.get).map { resp =>
          resp.status mustEqual Status.BadRequest
        }
      }
    }

    "POST /posts/{id}/comments" >> {
      "rejects empty name" >> {
        val routes = buildRoutes[IO](createCommentResult = Some(testCreatedComment))
        val body = CreateCommentRequest("", "test@example.com", "Comment text", None)
        val request = Request[IO](Method.POST, Uri.unsafeFromString(s"$v1/posts/1/comments"))
          .withEntity(body.asJson)

        for {
          resp <- ErrorHandler(routes.routes).run(request).value.map(_.get)
          respBody <- resp.as[String]
        } yield {
          resp.status mustEqual Status.BadRequest
          respBody must contain("\"code\":\"VALIDATION_ERROR\"")
          respBody must contain("\"name\"")
        }
      }

      "rejects invalid email" >> {
        val routes = buildRoutes[IO](createCommentResult = Some(testCreatedComment))
        val body = CreateCommentRequest("Author", "not-an-email", "Comment text", None)
        val request = Request[IO](Method.POST, Uri.unsafeFromString(s"$v1/posts/1/comments"))
          .withEntity(body.asJson)

        for {
          resp <- ErrorHandler(routes.routes).run(request).value.map(_.get)
          respBody <- resp.as[String]
        } yield {
          resp.status mustEqual Status.BadRequest
          respBody must contain("\"email\"")
        }
      }

      "rejects empty text" >> {
        val routes = buildRoutes[IO](createCommentResult = Some(testCreatedComment))
        val body = CreateCommentRequest("Author", "test@example.com", "", None)
        val request = Request[IO](Method.POST, Uri.unsafeFromString(s"$v1/posts/1/comments"))
          .withEntity(body.asJson)

        for {
          resp <- ErrorHandler(routes.routes).run(request).value.map(_.get)
          respBody <- resp.as[String]
        } yield {
          resp.status mustEqual Status.BadRequest
          respBody must contain("\"text\"")
        }
      }

      "sanitizes HTML in text" >> {
        val routes = buildRoutes[IO]()
        val body =
          CreateCommentRequest("Author", "test@example.com", "<script>alert('xss')</script>", None)
        val request = Request[IO](Method.POST, Uri.unsafeFromString(s"$v1/posts/1/comments"))
          .withEntity(body.asJson)

        for {
          resp <- routes.routes.run(request).value.map(_.get)
          respBody <- resp.as[String]
        } yield {
          resp.status mustEqual Status.Created
          respBody must not contain "<script>"
        }
      }

      "accumulates multiple errors" >> {
        val routes = buildRoutes[IO](createCommentResult = Some(testCreatedComment))
        val body = CreateCommentRequest("", "invalid", "", None)
        val request = Request[IO](Method.POST, Uri.unsafeFromString(s"$v1/posts/1/comments"))
          .withEntity(body.asJson)

        for {
          resp <- ErrorHandler(routes.routes).run(request).value.map(_.get)
          respBody <- resp.as[String]
        } yield {
          resp.status mustEqual Status.BadRequest
          respBody must contain("\"name\"")
          respBody must contain("\"email\"")
          respBody must contain("\"text\"")
        }
      }
    }

    "POST /contact" >> {
      "rejects invalid fields" >> {
        val routes = buildRoutes[IO]()
        val body = CreateContactRequest("", "invalid", "ab", "short", None)
        val request =
          Request[IO](Method.POST, Uri.unsafeFromString(s"$v1/contact")).withEntity(body.asJson)

        for {
          resp <- ErrorHandler(routes.routes).run(request).value.map(_.get)
          respBody <- resp.as[String]
        } yield {
          resp.status mustEqual Status.BadRequest
          respBody must contain("\"code\":\"VALIDATION_ERROR\"")
        }
      }
    }
  }

}
