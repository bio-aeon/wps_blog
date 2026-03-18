package su.wps.blog.endpoints

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import io.circe.syntax.*
import org.http4s.*
import org.http4s.circe.*
import org.http4s.implicits.*
import org.specs2.mutable.Specification
import su.wps.blog.models.api.*

class CommentRoutesSpec extends Specification with RoutesSpecSupport {

  "Comment routes" >> {
    "GET /posts/{id}/comments" >> {
      "returns threaded comments" >> {
        val routes = buildRoutes[IO](commentsResult = testCommentsResult)
        val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/posts/1/comments"))

        val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()

        resp.status mustEqual Status.Ok
      }

      "returns comments array and total" >> {
        val routes = buildRoutes[IO](commentsResult = testCommentsResult)
        val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/posts/1/comments"))

        val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
        val respBody = resp.as[String].unsafeRunSync()

        respBody must contain("\"comments\":")
        respBody must contain("\"total\":2")
      }

      "returns nested replies structure" >> {
        val routes = buildRoutes[IO](commentsResult = testCommentsResult)
        val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/posts/1/comments"))

        val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
        val respBody = resp.as[String].unsafeRunSync()

        respBody must contain("\"replies\":")
        respBody must contain("Reply text")
      }

      "returns empty result for post with no comments" >> {
        val routes = buildRoutes[IO]()
        val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/posts/99999/comments"))

        val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
        val respBody = resp.as[String].unsafeRunSync()

        resp.status mustEqual Status.Ok
        respBody mustEqual """{"comments":[],"total":0}"""
      }
    }

    "POST /posts/{id}/comments" >> {
      "returns 201 Created" >> {
        val routes = buildRoutes[IO](createCommentResult = Some(testCreatedComment))
        val body = CreateCommentRequest("Author", "test@example.com", "Comment text", None)
        val request = Request[IO](Method.POST, Uri.unsafeFromString(s"$v1/posts/1/comments"))
          .withEntity(body.asJson)

        val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()

        resp.status mustEqual Status.Created
      }

      "returns created comment in body" >> {
        val routes = buildRoutes[IO](createCommentResult = Some(testCreatedComment))
        val body = CreateCommentRequest("Author", "test@example.com", "Comment text", None)
        val request = Request[IO](Method.POST, Uri.unsafeFromString(s"$v1/posts/1/comments"))
          .withEntity(body.asJson)

        val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
        val respBody = resp.as[String].unsafeRunSync()

        respBody must contain("\"name\":\"Author\"")
        respBody must contain("\"text\":\"Comment text\"")
      }

      "returns comment with id" >> {
        val routes = buildRoutes[IO](createCommentResult = Some(testCreatedComment))
        val body = CreateCommentRequest("Author", "test@example.com", "Comment text", None)
        val request = Request[IO](Method.POST, Uri.unsafeFromString(s"$v1/posts/1/comments"))
          .withEntity(body.asJson)

        val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
        val respBody = resp.as[String].unsafeRunSync()

        respBody must contain("\"id\":")
      }

      "creates reply with parent id" >> {
        val routes = buildRoutes[IO](createCommentResult = Some(testCreatedComment))
        val body = CreateCommentRequest("Replier", "reply@example.com", "Reply text", Some(1))
        val request = Request[IO](Method.POST, Uri.unsafeFromString(s"$v1/posts/1/comments"))
          .withEntity(body.asJson)

        val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()

        resp.status mustEqual Status.Created
      }
    }

    "POST /comments/{id}/rate" >> {
      "returns 204 on success" >> {
        val routes = buildRoutes[IO]()
        val body = RateCommentRequest(isUpvote = true)
        val request = Request[IO](Method.POST, Uri.unsafeFromString(s"$v1/comments/1/rate"))
          .withEntity(body.asJson)

        val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()

        resp.status mustEqual Status.NoContent
      }

      "handles upvote" >> {
        val routes = buildRoutes[IO]()
        val body = RateCommentRequest(isUpvote = true)
        val request = Request[IO](Method.POST, Uri.unsafeFromString(s"$v1/comments/1/rate"))
          .withEntity(body.asJson)

        val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()

        resp.status mustEqual Status.NoContent
      }

      "handles downvote" >> {
        val routes = buildRoutes[IO]()
        val body = RateCommentRequest(isUpvote = false)
        val request = Request[IO](Method.POST, Uri.unsafeFromString(s"$v1/comments/1/rate"))
          .withEntity(body.asJson)

        val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()

        resp.status mustEqual Status.NoContent
      }

      "returns 204 for non-existent comment (idempotent)" >> {
        val routes = buildRoutes[IO]()
        val body = RateCommentRequest(isUpvote = true)
        val request = Request[IO](Method.POST, Uri.unsafeFromString(s"$v1/comments/99999/rate"))
          .withEntity(body.asJson)

        val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()

        resp.status mustEqual Status.NoContent
      }
    }
  }

}
