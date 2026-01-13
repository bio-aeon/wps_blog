package su.wps.blog.endpoints

import cats.effect.Concurrent
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import io.circe.syntax.*
import org.http4s.*
import org.http4s.circe.*
import org.http4s.implicits.*
import org.specs2.mutable.Specification
import su.wps.blog.endpoints.mocks.{CommentServiceMock, PostServiceMock}
import io.circe.Json
import su.wps.blog.models.api.*
import su.wps.blog.models.domain.{AppErr, CommentId, PostId, TagId}
import tofu.Raise

import java.time.ZonedDateTime

class RoutesSpec extends Specification {

  "Api routes should" >> {
    "return correct code and body for posts list retrieving" >> {
      val routes = mkRoutes[IO]
      val request =
        Request[IO](Method.GET, uri"posts".withQueryParams(Map("limit" -> "10", "offset" -> "0")))

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      val respBody = resp.as[String].unsafeRunSync()

      resp.status mustEqual Status.Ok
      respBody mustEqual """{"items":[{"id":1,"name":"name","short_text":"text","created_at":"2001-01-01T09:15:00Z","tags":[{"id":1,"name":"scala","slug":"scala"}]}],"total":1}"""
    }

    "return correct code and body for post retrieving by id" >> {
      val routes = mkRoutes[IO]
      val request = Request[IO](Method.GET, uri"posts/1")

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      val respBody = resp.as[String].unsafeRunSync()

      resp.status mustEqual Status.Ok
      respBody mustEqual """{"name":"name","text":"text","created_at":"2001-01-01T09:15:00Z","tags":[{"id":1,"name":"scala","slug":"scala"}]}"""
    }

    "return 200 with filtered posts when tag parameter is provided" >> {
      val routes = mkRoutesWithTagFilter[IO]
      val request = Request[IO](
        Method.GET,
        uri"posts".withQueryParams(Map("limit" -> "10", "offset" -> "0", "tag" -> "scala"))
      )

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      resp.status mustEqual Status.Ok
    }

    "return posts tagged with the specified tag" >> {
      val routes = mkRoutesWithTagFilter[IO]
      val request = Request[IO](
        Method.GET,
        uri"posts".withQueryParams(Map("limit" -> "10", "offset" -> "0", "tag" -> "scala"))
      )

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      val respBody = resp.as[String].unsafeRunSync()

      respBody must contain("\"slug\":\"scala\"")
    }

    "return all posts when tag parameter is not provided" >> {
      val routes = mkRoutesWithTagFilter[IO]
      val request =
        Request[IO](Method.GET, uri"posts".withQueryParams(Map("limit" -> "10", "offset" -> "0")))

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      val respBody = resp.as[String].unsafeRunSync()

      resp.status mustEqual Status.Ok
      respBody must contain("\"total\":1")
    }

    "return 204 No Content on successful view increment" >> {
      val routes = mkRoutes[IO]
      val request = Request[IO](Method.POST, uri"posts/1/view")

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()

      resp.status mustEqual Status.NoContent
    }

    "return 204 even for non-existent post (idempotent)" >> {
      val routes = mkRoutes[IO]
      val request = Request[IO](Method.POST, uri"posts/99999/view")

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()

      resp.status mustEqual Status.NoContent
    }

    "return 200 with search results for GET /posts/search" >> {
      val routes = mkRoutesWithSearch[IO]
      val request = Request[IO](
        Method.GET,
        uri"posts/search".withQueryParams(Map("q" -> "scala", "limit" -> "10", "offset" -> "0"))
      )

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      resp.status mustEqual Status.Ok
    }

    "return matching posts in search response" >> {
      val routes = mkRoutesWithSearch[IO]
      val request = Request[IO](
        Method.GET,
        uri"posts/search".withQueryParams(Map("q" -> "scala", "limit" -> "10", "offset" -> "0"))
      )

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      val respBody = resp.as[String].unsafeRunSync()

      respBody must contain("scala-tutorial")
      respBody must contain("\"total\":2")
    }

    "return empty list when search has no matches" >> {
      val routes = mkRoutesWithEmptySearch[IO]
      val request = Request[IO](
        Method.GET,
        uri"posts/search".withQueryParams(
          Map("q" -> "nonexistent", "limit" -> "10", "offset" -> "0")
        )
      )

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      val respBody = resp.as[String].unsafeRunSync()

      resp.status mustEqual Status.Ok
      respBody mustEqual """{"items":[],"total":0}"""
    }

    "return 200 with recent posts for GET /posts/recent" >> {
      val routes = mkRoutesWithRecentPosts[IO]
      val request = Request[IO](Method.GET, uri"posts/recent")

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      resp.status mustEqual Status.Ok
    }

    "return recent posts with default count when count param is not provided" >> {
      val routes = mkRoutesWithRecentPosts[IO]
      val request = Request[IO](Method.GET, uri"posts/recent")

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      val respBody = resp.as[String].unsafeRunSync()

      resp.status mustEqual Status.Ok
      respBody must contain("recent-post")
    }

    "return recent posts with specified count" >> {
      val routes = mkRoutesWithRecentPosts[IO]
      val request = Request[IO](Method.GET, uri"posts/recent".withQueryParams(Map("count" -> "3")))

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      resp.status mustEqual Status.Ok
    }

    "return empty list when no recent posts exist" >> {
      val routes = mkRoutesWithEmptyRecentPosts[IO]
      val request = Request[IO](Method.GET, uri"posts/recent")

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      val respBody = resp.as[String].unsafeRunSync()

      resp.status mustEqual Status.Ok
      respBody mustEqual "[]"
    }

    "return 200 with threaded comments for GET /posts/{id}/comments" >> {
      val routes = mkRoutesWithComments[IO]
      val request = Request[IO](Method.GET, uri"posts/1/comments")

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()

      resp.status mustEqual Status.Ok
    }

    "return JSON with comments array and total" >> {
      val routes = mkRoutesWithComments[IO]
      val request = Request[IO](Method.GET, uri"posts/1/comments")

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      val respBody = resp.as[String].unsafeRunSync()

      respBody must contain("\"comments\":")
      respBody must contain("\"total\":2")
    }

    "return nested replies structure in comments" >> {
      val routes = mkRoutesWithComments[IO]
      val request = Request[IO](Method.GET, uri"posts/1/comments")

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      val respBody = resp.as[String].unsafeRunSync()

      respBody must contain("\"replies\":")
      respBody must contain("Reply text")
    }

    "return empty comments for post with no comments" >> {
      val routes = mkRoutesWithEmptyComments[IO]
      val request = Request[IO](Method.GET, uri"posts/99999/comments")

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      val respBody = resp.as[String].unsafeRunSync()

      resp.status mustEqual Status.Ok
      respBody mustEqual """{"comments":[],"total":0}"""
    }

    "return 201 Created when creating a new comment" >> {
      val routes = mkRoutesForCreateComment[IO]
      val body = CreateCommentRequest("Author", "test@example.com", "Comment text", None)
      val request = Request[IO](Method.POST, uri"posts/1/comments").withEntity(body.asJson)

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()

      resp.status mustEqual Status.Created
    }

    "return created comment in response body" >> {
      val routes = mkRoutesForCreateComment[IO]
      val body = CreateCommentRequest("Author", "test@example.com", "Comment text", None)
      val request = Request[IO](Method.POST, uri"posts/1/comments").withEntity(body.asJson)

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      val respBody = resp.as[String].unsafeRunSync()

      respBody must contain("\"name\":\"Author\"")
      respBody must contain("\"text\":\"Comment text\"")
    }

    "return comment with id in response" >> {
      val routes = mkRoutesForCreateComment[IO]
      val body = CreateCommentRequest("Author", "test@example.com", "Comment text", None)
      val request = Request[IO](Method.POST, uri"posts/1/comments").withEntity(body.asJson)

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      val respBody = resp.as[String].unsafeRunSync()

      respBody must contain("\"id\":")
    }

    "create reply comment with parent id" >> {
      val routes = mkRoutesForCreateComment[IO]
      val body = CreateCommentRequest("Replier", "reply@example.com", "Reply text", Some(1))
      val request = Request[IO](Method.POST, uri"posts/1/comments").withEntity(body.asJson)

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()

      resp.status mustEqual Status.Created
    }

    "return 204 No Content on successful comment rating" >> {
      val routes = mkRoutesForRateComment[IO]
      val body = RateCommentRequest(isUpvote = true)
      val request = Request[IO](Method.POST, uri"comments/1/rate").withEntity(body.asJson)

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()

      resp.status mustEqual Status.NoContent
    }

    "handle upvote rating request" >> {
      val routes = mkRoutesForRateComment[IO]
      val body = RateCommentRequest(isUpvote = true)
      val request = Request[IO](Method.POST, uri"comments/1/rate").withEntity(body.asJson)

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()

      resp.status mustEqual Status.NoContent
    }

    "handle downvote rating request" >> {
      val routes = mkRoutesForRateComment[IO]
      val body = RateCommentRequest(isUpvote = false)
      val request = Request[IO](Method.POST, uri"comments/1/rate").withEntity(body.asJson)

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()

      resp.status mustEqual Status.NoContent
    }

    "return 204 even for non-existent comment (idempotent)" >> {
      val routes = mkRoutesForRateComment[IO]
      val body = RateCommentRequest(isUpvote = true)
      val request = Request[IO](Method.POST, uri"comments/99999/rate").withEntity(body.asJson)

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()

      resp.status mustEqual Status.NoContent
    }

    "return 204 No Content on successful comment deletion" >> {
      val routes = mkRoutesForCommentModeration[IO]
      val request = Request[IO](Method.DELETE, uri"admin/comments/1")

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()

      resp.status mustEqual Status.NoContent
    }

    "return 204 No Content on successful comment approval" >> {
      val routes = mkRoutesForCommentModeration[IO]
      val request = Request[IO](Method.PUT, uri"admin/comments/1/approve")

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()

      resp.status mustEqual Status.NoContent
    }
  }

  private def mkRoutes[F[_]: Concurrent: Raise[*[_], AppErr]]: Routes[F] = {
    val tags = List(TagResult(TagId(1), "scala", "scala"))
    val postService = PostServiceMock.create[F](
      List(
        ListPostResult(PostId(1), "name", "text", ZonedDateTime.parse("2001-01-01T09:15:00Z"), tags)
      ),
      Some(PostResult("name", "text", ZonedDateTime.parse("2001-01-01T09:15:00Z"), tags))
    )

    val commentService = CommentServiceMock.create[F]()
    RoutesImpl.create[F](postService, commentService)
  }

  private def mkRoutesWithTagFilter[F[_]: Concurrent: Raise[*[_], AppErr]]: Routes[F] = {
    val tags = List(TagResult(TagId(1), "scala", "scala"))
    val allPosts = List(
      ListPostResult(PostId(1), "name", "text", ZonedDateTime.parse("2001-01-01T09:15:00Z"), tags)
    )
    val taggedPosts = List(
      ListPostResult(
        PostId(2),
        "scala-post",
        "scala-text",
        ZonedDateTime.parse("2001-01-01T09:15:00Z"),
        tags
      )
    )
    val postService = PostServiceMock
      .create[F](allPostsResult = allPosts, postByIdResult = None, postsByTagResult = taggedPosts)
    val commentService = CommentServiceMock.create[F]()

    RoutesImpl.create[F](postService, commentService)
  }

  private def mkRoutesWithSearch[F[_]: Concurrent: Raise[*[_], AppErr]]: Routes[F] = {
    val tags = List(TagResult(TagId(1), "scala", "scala"))
    val searchResults = List(
      ListPostResult(
        PostId(1),
        "scala-tutorial",
        "Learn Scala programming",
        ZonedDateTime.parse("2001-01-01T09:15:00Z"),
        tags
      ),
      ListPostResult(
        PostId(2),
        "scala-advanced",
        "Advanced Scala topics",
        ZonedDateTime.parse("2001-01-02T09:15:00Z"),
        tags
      )
    )
    val postService = PostServiceMock.create[F](searchPostsResult = searchResults)
    val commentService = CommentServiceMock.create[F]()

    RoutesImpl.create[F](postService, commentService)
  }

  private def mkRoutesWithEmptySearch[F[_]: Concurrent: Raise[*[_], AppErr]]: Routes[F] = {
    val postService = PostServiceMock.create[F](searchPostsResult = Nil)
    val commentService = CommentServiceMock.create[F]()

    RoutesImpl.create[F](postService, commentService)
  }

  private def mkRoutesWithRecentPosts[F[_]: Concurrent: Raise[*[_], AppErr]]: Routes[F] = {
    val tags = List(TagResult(TagId(1), "scala", "scala"))
    val recentPosts = List(
      ListPostResult(
        PostId(1),
        "recent-post",
        "Recent post text",
        ZonedDateTime.parse("2001-01-01T09:15:00Z"),
        tags
      ),
      ListPostResult(
        PostId(2),
        "another-recent",
        "Another recent post",
        ZonedDateTime.parse("2001-01-02T09:15:00Z"),
        tags
      )
    )
    val postService = PostServiceMock.create[F](recentPostsResult = recentPosts)
    val commentService = CommentServiceMock.create[F]()

    RoutesImpl.create[F](postService, commentService)
  }

  private def mkRoutesWithEmptyRecentPosts[F[_]: Concurrent: Raise[*[_], AppErr]]: Routes[F] = {
    val postService = PostServiceMock.create[F](recentPostsResult = Nil)
    val commentService = CommentServiceMock.create[F]()

    RoutesImpl.create[F](postService, commentService)
  }

  private def mkRoutesWithComments[F[_]: Concurrent: Raise[*[_], AppErr]]: Routes[F] = {
    val postService = PostServiceMock.create[F]()
    val reply = CommentResult(
      CommentId(2),
      "Replier",
      "Reply text",
      1,
      ZonedDateTime.parse("2001-01-01T10:00:00Z"),
      Nil
    )
    val rootComment = CommentResult(
      CommentId(1),
      "Author",
      "Root comment",
      5,
      ZonedDateTime.parse("2001-01-01T09:00:00Z"),
      List(reply)
    )
    val commentService = CommentServiceMock.create[F](CommentsListResult(List(rootComment), 2))

    RoutesImpl.create[F](postService, commentService)
  }

  private def mkRoutesWithEmptyComments[F[_]: Concurrent: Raise[*[_], AppErr]]: Routes[F] = {
    val postService = PostServiceMock.create[F]()
    val commentService = CommentServiceMock.create[F](CommentsListResult(Nil, 0))

    RoutesImpl.create[F](postService, commentService)
  }

  private def mkRoutesForCreateComment[F[_]: Concurrent: Raise[*[_], AppErr]]: Routes[F] = {
    val postService = PostServiceMock.create[F]()
    val createdComment = CommentResult(
      CommentId(1),
      "Author",
      "Comment text",
      0,
      ZonedDateTime.parse("2001-01-01T09:00:00Z"),
      Nil
    )
    val commentService = CommentServiceMock.create[F](createCommentResult = Some(createdComment))

    RoutesImpl.create[F](postService, commentService)
  }

  private def mkRoutesForRateComment[F[_]: Concurrent: Raise[*[_], AppErr]]: Routes[F] = {
    val postService = PostServiceMock.create[F]()
    val commentService = CommentServiceMock.create[F]()

    RoutesImpl.create[F](postService, commentService)
  }

  private def mkRoutesForCommentModeration[F[_]: Concurrent: Raise[*[_], AppErr]]: Routes[F] = {
    val postService = PostServiceMock.create[F]()
    val commentService = CommentServiceMock.create[F]()

    RoutesImpl.create[F](postService, commentService)
  }
}
