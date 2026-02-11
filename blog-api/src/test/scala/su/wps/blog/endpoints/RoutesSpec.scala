package su.wps.blog.endpoints

import cats.effect.Concurrent
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import io.circe.syntax.*
import org.http4s.*
import org.http4s.circe.*
import org.http4s.implicits.*
import org.http4s.Uri
import org.specs2.mutable.Specification
import su.wps.blog.endpoints.mocks.*
import io.circe.Json
import su.wps.blog.models.api.*
import su.wps.blog.models.domain.{AppErr, CommentId, PostId, TagId}
import tofu.Raise

import java.time.ZonedDateTime

class RoutesSpec extends Specification {

  private val testTimestamp = ZonedDateTime.parse("2001-01-01T09:15:00Z")
  private val v1 = RoutesImpl.ApiVersion

  "Api routes should" >> {
    "return correct code and body for posts list retrieving" >> {
      val routes = mkRoutes[IO]
      val request =
        Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/posts").withQueryParams(Map("limit" -> "10", "offset" -> "0")))

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      val respBody = resp.as[String].unsafeRunSync()

      resp.status mustEqual Status.Ok
      respBody mustEqual """{"items":[{"id":1,"name":"name","short_text":"text","created_at":"2001-01-01T09:15:00Z","tags":[{"id":1,"name":"scala","slug":"scala"}]}],"total":1}"""
    }

    "return correct code and body for post retrieving by id" >> {
      val routes = mkRoutes[IO]
      val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/posts/1"))

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      val respBody = resp.as[String].unsafeRunSync()

      resp.status mustEqual Status.Ok
      respBody mustEqual """{"name":"name","text":"text","created_at":"2001-01-01T09:15:00Z","tags":[{"id":1,"name":"scala","slug":"scala"}]}"""
    }

    "return 200 with filtered posts when tag parameter is provided" >> {
      val routes = mkRoutesWithTagFilter[IO]
      val request = Request[IO](
        Method.GET,
        Uri.unsafeFromString(s"$v1/posts").withQueryParams(Map("limit" -> "10", "offset" -> "0", "tag" -> "scala"))
      )

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      resp.status mustEqual Status.Ok
    }

    "return posts tagged with the specified tag" >> {
      val routes = mkRoutesWithTagFilter[IO]
      val request = Request[IO](
        Method.GET,
        Uri.unsafeFromString(s"$v1/posts").withQueryParams(Map("limit" -> "10", "offset" -> "0", "tag" -> "scala"))
      )

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      val respBody = resp.as[String].unsafeRunSync()

      respBody must contain("\"slug\":\"scala\"")
    }

    "return all posts when tag parameter is not provided" >> {
      val routes = mkRoutesWithTagFilter[IO]
      val request =
        Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/posts").withQueryParams(Map("limit" -> "10", "offset" -> "0")))

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      val respBody = resp.as[String].unsafeRunSync()

      resp.status mustEqual Status.Ok
      respBody must contain("\"total\":1")
    }

    "return 204 No Content on successful view increment" >> {
      val routes = mkRoutes[IO]
      val request = Request[IO](Method.POST, Uri.unsafeFromString(s"$v1/posts/1/view"))

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()

      resp.status mustEqual Status.NoContent
    }

    "return 204 even for non-existent post (idempotent)" >> {
      val routes = mkRoutes[IO]
      val request = Request[IO](Method.POST, Uri.unsafeFromString(s"$v1/posts/99999/view"))

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()

      resp.status mustEqual Status.NoContent
    }

    "return 200 with search results for GET /posts/search" >> {
      val routes = mkRoutesWithSearch[IO]
      val request = Request[IO](
        Method.GET,
        Uri.unsafeFromString(s"$v1/posts/search").withQueryParams(Map("q" -> "scala", "limit" -> "10", "offset" -> "0"))
      )

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      resp.status mustEqual Status.Ok
    }

    "return matching posts in search response" >> {
      val routes = mkRoutesWithSearch[IO]
      val request = Request[IO](
        Method.GET,
        Uri.unsafeFromString(s"$v1/posts/search").withQueryParams(Map("q" -> "scala", "limit" -> "10", "offset" -> "0"))
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
        Uri.unsafeFromString(s"$v1/posts/search").withQueryParams(
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
      val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/posts/recent"))

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      resp.status mustEqual Status.Ok
    }

    "return recent posts with default count when count param is not provided" >> {
      val routes = mkRoutesWithRecentPosts[IO]
      val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/posts/recent"))

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      val respBody = resp.as[String].unsafeRunSync()

      resp.status mustEqual Status.Ok
      respBody must contain("recent-post")
    }

    "return recent posts with specified count" >> {
      val routes = mkRoutesWithRecentPosts[IO]
      val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/posts/recent").withQueryParams(Map("count" -> "3")))

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      resp.status mustEqual Status.Ok
    }

    "return empty list when no recent posts exist" >> {
      val routes = mkRoutesWithEmptyRecentPosts[IO]
      val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/posts/recent"))

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      val respBody = resp.as[String].unsafeRunSync()

      resp.status mustEqual Status.Ok
      respBody mustEqual "[]"
    }

    "return 200 with threaded comments for GET /posts/{id}/comments" >> {
      val routes = mkRoutesWithComments[IO]
      val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/posts/1/comments"))

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()

      resp.status mustEqual Status.Ok
    }

    "return JSON with comments array and total" >> {
      val routes = mkRoutesWithComments[IO]
      val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/posts/1/comments"))

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      val respBody = resp.as[String].unsafeRunSync()

      respBody must contain("\"comments\":")
      respBody must contain("\"total\":2")
    }

    "return nested replies structure in comments" >> {
      val routes = mkRoutesWithComments[IO]
      val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/posts/1/comments"))

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      val respBody = resp.as[String].unsafeRunSync()

      respBody must contain("\"replies\":")
      respBody must contain("Reply text")
    }

    "return empty comments for post with no comments" >> {
      val routes = mkRoutesWithEmptyComments[IO]
      val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/posts/99999/comments"))

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      val respBody = resp.as[String].unsafeRunSync()

      resp.status mustEqual Status.Ok
      respBody mustEqual """{"comments":[],"total":0}"""
    }

    "return 201 Created when creating a new comment" >> {
      val routes = mkRoutesForCreateComment[IO]
      val body = CreateCommentRequest("Author", "test@example.com", "Comment text", None)
      val request = Request[IO](Method.POST, Uri.unsafeFromString(s"$v1/posts/1/comments")).withEntity(body.asJson)

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()

      resp.status mustEqual Status.Created
    }

    "return created comment in response body" >> {
      val routes = mkRoutesForCreateComment[IO]
      val body = CreateCommentRequest("Author", "test@example.com", "Comment text", None)
      val request = Request[IO](Method.POST, Uri.unsafeFromString(s"$v1/posts/1/comments")).withEntity(body.asJson)

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      val respBody = resp.as[String].unsafeRunSync()

      respBody must contain("\"name\":\"Author\"")
      respBody must contain("\"text\":\"Comment text\"")
    }

    "return comment with id in response" >> {
      val routes = mkRoutesForCreateComment[IO]
      val body = CreateCommentRequest("Author", "test@example.com", "Comment text", None)
      val request = Request[IO](Method.POST, Uri.unsafeFromString(s"$v1/posts/1/comments")).withEntity(body.asJson)

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      val respBody = resp.as[String].unsafeRunSync()

      respBody must contain("\"id\":")
    }

    "create reply comment with parent id" >> {
      val routes = mkRoutesForCreateComment[IO]
      val body = CreateCommentRequest("Replier", "reply@example.com", "Reply text", Some(1))
      val request = Request[IO](Method.POST, Uri.unsafeFromString(s"$v1/posts/1/comments")).withEntity(body.asJson)

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()

      resp.status mustEqual Status.Created
    }

    "return 204 No Content on successful comment rating" >> {
      val routes = mkRoutesForRateComment[IO]
      val body = RateCommentRequest(isUpvote = true)
      val request = Request[IO](Method.POST, Uri.unsafeFromString(s"$v1/comments/1/rate")).withEntity(body.asJson)

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()

      resp.status mustEqual Status.NoContent
    }

    "handle upvote rating request" >> {
      val routes = mkRoutesForRateComment[IO]
      val body = RateCommentRequest(isUpvote = true)
      val request = Request[IO](Method.POST, Uri.unsafeFromString(s"$v1/comments/1/rate")).withEntity(body.asJson)

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()

      resp.status mustEqual Status.NoContent
    }

    "handle downvote rating request" >> {
      val routes = mkRoutesForRateComment[IO]
      val body = RateCommentRequest(isUpvote = false)
      val request = Request[IO](Method.POST, Uri.unsafeFromString(s"$v1/comments/1/rate")).withEntity(body.asJson)

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()

      resp.status mustEqual Status.NoContent
    }

    "return 204 even for non-existent comment (idempotent)" >> {
      val routes = mkRoutesForRateComment[IO]
      val body = RateCommentRequest(isUpvote = true)
      val request = Request[IO](Method.POST, Uri.unsafeFromString(s"$v1/comments/99999/rate")).withEntity(body.asJson)

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()

      resp.status mustEqual Status.NoContent
    }

    "return 204 No Content on successful comment deletion" >> {
      val routes = mkRoutesForCommentModeration[IO]
      val request = Request[IO](Method.DELETE, Uri.unsafeFromString(s"$v1/admin/comments/1"))

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()

      resp.status mustEqual Status.NoContent
    }

    "return 204 No Content on successful comment approval" >> {
      val routes = mkRoutesForCommentModeration[IO]
      val request = Request[IO](Method.PUT, Uri.unsafeFromString(s"$v1/admin/comments/1/approve"))

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()

      resp.status mustEqual Status.NoContent
    }

    "return 200 with tags list for GET /tags" >> {
      val routes = mkRoutesWithTags[IO]
      val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/tags"))

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()

      resp.status mustEqual Status.Ok
    }

    "return tags with post counts in response" >> {
      val routes = mkRoutesWithTags[IO]
      val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/tags"))

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      val respBody = resp.as[String].unsafeRunSync()

      respBody must contain("\"post_count\":")
      respBody must contain("\"total\":2")
    }

    "return empty tags list when no tags exist" >> {
      val routes = mkRoutesWithEmptyTags[IO]
      val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/tags"))

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      val respBody = resp.as[String].unsafeRunSync()

      resp.status mustEqual Status.Ok
      respBody mustEqual """{"items":[],"total":0}"""
    }

    "return 200 with tag cloud for GET /tags/cloud" >> {
      val routes = mkRoutesWithTagCloud[IO]
      val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/tags/cloud"))

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()

      resp.status mustEqual Status.Ok
    }

    "return tag cloud with normalized weights" >> {
      val routes = mkRoutesWithTagCloud[IO]
      val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/tags/cloud"))

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      val respBody = resp.as[String].unsafeRunSync()

      respBody must contain("\"weight\":")
      respBody must contain("\"count\":")
      respBody must contain("\"tags\":")
    }

    "return empty tag cloud when no tags exist" >> {
      val routes = mkRoutesWithEmptyTagCloud[IO]
      val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/tags/cloud"))

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      val respBody = resp.as[String].unsafeRunSync()

      resp.status mustEqual Status.Ok
      respBody mustEqual """{"tags":[]}"""
    }

    "return 200 with page content for GET /pages/{url}" >> {
      val routes = mkRoutesWithPage[IO]
      val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/pages/about"))

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()

      resp.status mustEqual Status.Ok
    }

    "return page with correct fields in response" >> {
      val routes = mkRoutesWithPage[IO]
      val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/pages/about"))

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      val respBody = resp.as[String].unsafeRunSync()

      respBody must contain("\"url\":\"about\"")
      respBody must contain("\"title\":\"About Us\"")
      respBody must contain("\"content\":\"About page content\"")
    }

    "return 404 with error response when page is not found" >> {
      val routes = mkRoutesWithNoPage[IO]
      val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/pages/non-existent"))

      val resp =
        ErrorHandler(routes.routes).run(request).value.map(_.get).unsafeRunSync()

      resp.status mustEqual Status.NotFound
      val body = resp.as[String].unsafeRunSync()
      body must contain("\"code\":\"NOT_FOUND\"")
      body must contain("Page not found")
    }

    "return 200 with pages list for GET /pages" >> {
      val routes = mkRoutesWithPagesList[IO]
      val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/pages"))

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()

      resp.status mustEqual Status.Ok
    }

    "return pages with url and title in response" >> {
      val routes = mkRoutesWithPagesList[IO]
      val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/pages"))

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      val respBody = resp.as[String].unsafeRunSync()

      respBody must contain("\"url\":\"about\"")
      respBody must contain("\"title\":\"About Us\"")
      respBody must contain("\"total\":2")
    }

    "return empty pages list when no pages exist" >> {
      val routes = mkRoutesWithEmptyPagesList[IO]
      val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/pages"))

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      val respBody = resp.as[String].unsafeRunSync()

      resp.status mustEqual Status.Ok
      respBody mustEqual """{"items":[],"total":0}"""
    }

    "return 200 with health status for GET /health" >> {
      val routes = mkRoutesWithHealth[IO]
      val request = Request[IO](Method.GET, uri"health")

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()

      resp.status mustEqual Status.Ok
    }

    "return health response with status, database and timestamp fields" >> {
      val routes = mkRoutesWithHealth[IO]
      val request = Request[IO](Method.GET, uri"health")

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      val respBody = resp.as[String].unsafeRunSync()

      respBody must contain("\"status\":\"healthy\"")
      respBody must contain("\"database\":\"healthy\"")
      respBody must contain("\"timestamp\":")
    }

    "return degraded status when database is unhealthy" >> {
      val routes = mkRoutesWithUnhealthyDb[IO]
      val request = Request[IO](Method.GET, uri"health")

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      val respBody = resp.as[String].unsafeRunSync()

      resp.status mustEqual Status.Ok
      respBody must contain("\"status\":\"degraded\"")
      respBody must contain("\"database\":\"unhealthy\"")
    }

    "return 400 for invalid pagination limit on GET /posts" >> {
      val routes = mkRoutes[IO]
      val request = Request[IO](
        Method.GET,
        Uri.unsafeFromString(s"$v1/posts").withQueryParams(Map("limit" -> "0", "offset" -> "0"))
      )

      val resp =
        ErrorHandler(routes.routes).run(request).value.map(_.get).unsafeRunSync()

      resp.status mustEqual Status.BadRequest
      val body = resp.as[String].unsafeRunSync()
      body must contain("\"code\":\"VALIDATION_ERROR\"")
      body must contain("\"limit\"")
    }

    "return 400 for negative pagination offset on GET /posts" >> {
      val routes = mkRoutes[IO]
      val request = Request[IO](
        Method.GET,
        Uri.unsafeFromString(s"$v1/posts").withQueryParams(Map("limit" -> "10", "offset" -> "-1"))
      )

      val resp =
        ErrorHandler(routes.routes).run(request).value.map(_.get).unsafeRunSync()

      resp.status mustEqual Status.BadRequest
      val body = resp.as[String].unsafeRunSync()
      body must contain("\"code\":\"VALIDATION_ERROR\"")
      body must contain("\"offset\"")
    }

    "return 400 for limit exceeding maximum on GET /posts" >> {
      val routes = mkRoutes[IO]
      val request = Request[IO](
        Method.GET,
        Uri.unsafeFromString(s"$v1/posts").withQueryParams(Map("limit" -> "101", "offset" -> "0"))
      )

      val resp =
        ErrorHandler(routes.routes).run(request).value.map(_.get).unsafeRunSync()

      resp.status mustEqual Status.BadRequest
    }

    "return 400 for invalid pagination on GET /posts/search" >> {
      val routes = mkRoutesWithSearch[IO]
      val request = Request[IO](
        Method.GET,
        Uri.unsafeFromString(s"$v1/posts/search").withQueryParams(
          Map("q" -> "scala", "limit" -> "-1", "offset" -> "0")
        )
      )

      val resp =
        ErrorHandler(routes.routes).run(request).value.map(_.get).unsafeRunSync()

      resp.status mustEqual Status.BadRequest
    }

    "return 400 for empty comment name on POST /posts/{id}/comments" >> {
      val routes = mkRoutesForCreateComment[IO]
      val body = CreateCommentRequest("", "test@example.com", "Comment text", None)
      val request = Request[IO](Method.POST, Uri.unsafeFromString(s"$v1/posts/1/comments")).withEntity(body.asJson)

      val resp =
        ErrorHandler(routes.routes).run(request).value.map(_.get).unsafeRunSync()

      resp.status mustEqual Status.BadRequest
      val respBody = resp.as[String].unsafeRunSync()
      respBody must contain("\"code\":\"VALIDATION_ERROR\"")
      respBody must contain("\"name\"")
    }

    "return 400 for invalid email on POST /posts/{id}/comments" >> {
      val routes = mkRoutesForCreateComment[IO]
      val body = CreateCommentRequest("Author", "not-an-email", "Comment text", None)
      val request = Request[IO](Method.POST, Uri.unsafeFromString(s"$v1/posts/1/comments")).withEntity(body.asJson)

      val resp =
        ErrorHandler(routes.routes).run(request).value.map(_.get).unsafeRunSync()

      resp.status mustEqual Status.BadRequest
      val respBody = resp.as[String].unsafeRunSync()
      respBody must contain("\"email\"")
    }

    "return 400 for empty comment text on POST /posts/{id}/comments" >> {
      val routes = mkRoutesForCreateComment[IO]
      val body = CreateCommentRequest("Author", "test@example.com", "", None)
      val request = Request[IO](Method.POST, Uri.unsafeFromString(s"$v1/posts/1/comments")).withEntity(body.asJson)

      val resp =
        ErrorHandler(routes.routes).run(request).value.map(_.get).unsafeRunSync()

      resp.status mustEqual Status.BadRequest
      val respBody = resp.as[String].unsafeRunSync()
      respBody must contain("\"text\"")
    }

    "sanitize HTML in comment text on POST /posts/{id}/comments" >> {
      val routes = mkRoutesForCreateCommentEcho[IO]
      val body =
        CreateCommentRequest("Author", "test@example.com", "<script>alert('xss')</script>", None)
      val request = Request[IO](Method.POST, Uri.unsafeFromString(s"$v1/posts/1/comments")).withEntity(body.asJson)

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()

      resp.status mustEqual Status.Created
      val respBody = resp.as[String].unsafeRunSync()
      respBody must not contain "<script>"
    }

    "accumulate multiple validation errors on POST /posts/{id}/comments" >> {
      val routes = mkRoutesForCreateComment[IO]
      val body = CreateCommentRequest("", "invalid", "", None)
      val request = Request[IO](Method.POST, Uri.unsafeFromString(s"$v1/posts/1/comments")).withEntity(body.asJson)

      val resp =
        ErrorHandler(routes.routes).run(request).value.map(_.get).unsafeRunSync()

      resp.status mustEqual Status.BadRequest
      val respBody = resp.as[String].unsafeRunSync()
      respBody must contain("\"name\"")
      respBody must contain("\"email\"")
      respBody must contain("\"text\"")
    }

    "serve API routes under /v1 prefix" >> {
      val routes = mkRoutes[IO]
      val request = Request[IO](
        Method.GET,
        Uri.unsafeFromString(s"$v1/posts").withQueryParams(Map("limit" -> "10", "offset" -> "0"))
      )

      val resp = routes.routes.run(request).value.unsafeRunSync()

      resp must beSome
      resp.get.status mustEqual Status.Ok
    }

    "not serve API routes without version prefix" >> {
      val routes = mkRoutes[IO]
      val request = Request[IO](
        Method.GET,
        uri"posts".withQueryParams(Map("limit" -> "10", "offset" -> "0"))
      )

      val resp = routes.routes.run(request).value.unsafeRunSync()

      resp must beNone
    }

    "serve health endpoint without version prefix" >> {
      val routes = mkRoutesWithHealth[IO]
      val request = Request[IO](Method.GET, uri"health")

      val resp = routes.routes.run(request).value.unsafeRunSync()

      resp must beSome
      resp.get.status mustEqual Status.Ok
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
    val tagService = TagServiceMock.create[F]()
    val pageService = PageServiceMock.create[F]()
    val healthService = HealthServiceMock.create[F](timestamp = testTimestamp)
    RoutesImpl.create[F](postService, commentService, tagService, pageService, healthService)
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
    val tagService = TagServiceMock.create[F]()
    val pageService = PageServiceMock.create[F]()

    val healthService = HealthServiceMock.create[F](timestamp = testTimestamp)
    RoutesImpl.create[F](postService, commentService, tagService, pageService, healthService)
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
    val tagService = TagServiceMock.create[F]()
    val pageService = PageServiceMock.create[F]()

    val healthService = HealthServiceMock.create[F](timestamp = testTimestamp)
    RoutesImpl.create[F](postService, commentService, tagService, pageService, healthService)
  }

  private def mkRoutesWithEmptySearch[F[_]: Concurrent: Raise[*[_], AppErr]]: Routes[F] = {
    val postService = PostServiceMock.create[F](searchPostsResult = Nil)
    val commentService = CommentServiceMock.create[F]()
    val tagService = TagServiceMock.create[F]()
    val pageService = PageServiceMock.create[F]()

    val healthService = HealthServiceMock.create[F](timestamp = testTimestamp)
    RoutesImpl.create[F](postService, commentService, tagService, pageService, healthService)
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
    val tagService = TagServiceMock.create[F]()
    val pageService = PageServiceMock.create[F]()

    val healthService = HealthServiceMock.create[F](timestamp = testTimestamp)
    RoutesImpl.create[F](postService, commentService, tagService, pageService, healthService)
  }

  private def mkRoutesWithEmptyRecentPosts[F[_]: Concurrent: Raise[*[_], AppErr]]: Routes[F] = {
    val postService = PostServiceMock.create[F](recentPostsResult = Nil)
    val commentService = CommentServiceMock.create[F]()
    val tagService = TagServiceMock.create[F]()
    val pageService = PageServiceMock.create[F]()

    val healthService = HealthServiceMock.create[F](timestamp = testTimestamp)
    RoutesImpl.create[F](postService, commentService, tagService, pageService, healthService)
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
    val tagService = TagServiceMock.create[F]()
    val pageService = PageServiceMock.create[F]()

    val healthService = HealthServiceMock.create[F](timestamp = testTimestamp)
    RoutesImpl.create[F](postService, commentService, tagService, pageService, healthService)
  }

  private def mkRoutesWithEmptyComments[F[_]: Concurrent: Raise[*[_], AppErr]]: Routes[F] = {
    val postService = PostServiceMock.create[F]()
    val commentService = CommentServiceMock.create[F](CommentsListResult(Nil, 0))
    val tagService = TagServiceMock.create[F]()
    val pageService = PageServiceMock.create[F]()

    val healthService = HealthServiceMock.create[F](timestamp = testTimestamp)
    RoutesImpl.create[F](postService, commentService, tagService, pageService, healthService)
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
    val tagService = TagServiceMock.create[F]()
    val pageService = PageServiceMock.create[F]()

    val healthService = HealthServiceMock.create[F](timestamp = testTimestamp)
    RoutesImpl.create[F](postService, commentService, tagService, pageService, healthService)
  }

  private def mkRoutesForCreateCommentEcho[F[_]: Concurrent: Raise[*[_], AppErr]]: Routes[F] = {
    val postService = PostServiceMock.create[F]()
    val commentService = CommentServiceMock.create[F]()
    val tagService = TagServiceMock.create[F]()
    val pageService = PageServiceMock.create[F]()

    val healthService = HealthServiceMock.create[F](timestamp = testTimestamp)
    RoutesImpl.create[F](postService, commentService, tagService, pageService, healthService)
  }

  private def mkRoutesForRateComment[F[_]: Concurrent: Raise[*[_], AppErr]]: Routes[F] = {
    val postService = PostServiceMock.create[F]()
    val commentService = CommentServiceMock.create[F]()
    val tagService = TagServiceMock.create[F]()
    val pageService = PageServiceMock.create[F]()

    val healthService = HealthServiceMock.create[F](timestamp = testTimestamp)
    RoutesImpl.create[F](postService, commentService, tagService, pageService, healthService)
  }

  private def mkRoutesForCommentModeration[F[_]: Concurrent: Raise[*[_], AppErr]]: Routes[F] = {
    val postService = PostServiceMock.create[F]()
    val commentService = CommentServiceMock.create[F]()
    val tagService = TagServiceMock.create[F]()
    val pageService = PageServiceMock.create[F]()

    val healthService = HealthServiceMock.create[F](timestamp = testTimestamp)
    RoutesImpl.create[F](postService, commentService, tagService, pageService, healthService)
  }

  private def mkRoutesWithTags[F[_]: Concurrent: Raise[*[_], AppErr]]: Routes[F] = {
    val postService = PostServiceMock.create[F]()
    val commentService = CommentServiceMock.create[F]()
    val tagsWithCounts = List(
      TagWithCountResult(TagId(1), "scala", "scala", 10),
      TagWithCountResult(TagId(2), "rust", "rust", 5)
    )
    val tagService = TagServiceMock.create[F](tagsWithCounts)
    val pageService = PageServiceMock.create[F]()

    val healthService = HealthServiceMock.create[F](timestamp = testTimestamp)
    RoutesImpl.create[F](postService, commentService, tagService, pageService, healthService)
  }

  private def mkRoutesWithEmptyTags[F[_]: Concurrent: Raise[*[_], AppErr]]: Routes[F] = {
    val postService = PostServiceMock.create[F]()
    val commentService = CommentServiceMock.create[F]()
    val tagService = TagServiceMock.create[F]()
    val pageService = PageServiceMock.create[F]()

    val healthService = HealthServiceMock.create[F](timestamp = testTimestamp)
    RoutesImpl.create[F](postService, commentService, tagService, pageService, healthService)
  }

  private def mkRoutesWithTagCloud[F[_]: Concurrent: Raise[*[_], AppErr]]: Routes[F] = {
    val postService = PostServiceMock.create[F]()
    val commentService = CommentServiceMock.create[F]()
    val tagCloud = TagCloudResult(
      List(TagCloudItem("scala", "scala", 10, 1.0), TagCloudItem("rust", "rust", 5, 0.5))
    )
    val tagService = TagServiceMock.create[F](getTagCloudResult = tagCloud)
    val pageService = PageServiceMock.create[F]()

    val healthService = HealthServiceMock.create[F](timestamp = testTimestamp)
    RoutesImpl.create[F](postService, commentService, tagService, pageService, healthService)
  }

  private def mkRoutesWithEmptyTagCloud[F[_]: Concurrent: Raise[*[_], AppErr]]: Routes[F] = {
    val postService = PostServiceMock.create[F]()
    val commentService = CommentServiceMock.create[F]()
    val tagService = TagServiceMock.create[F](getTagCloudResult = TagCloudResult(Nil))
    val pageService = PageServiceMock.create[F]()

    val healthService = HealthServiceMock.create[F](timestamp = testTimestamp)
    RoutesImpl.create[F](postService, commentService, tagService, pageService, healthService)
  }

  private def mkRoutesWithPage[F[_]: Concurrent: Raise[*[_], AppErr]]: Routes[F] = {
    val postService = PostServiceMock.create[F]()
    val commentService = CommentServiceMock.create[F]()
    val tagService = TagServiceMock.create[F]()
    val page = PageResult(1, "about", "About Us", "About page content", ZonedDateTime.parse("2001-01-01T09:15:00Z"))
    val pageService = PageServiceMock.create[F](Some(page))

    val healthService = HealthServiceMock.create[F](timestamp = testTimestamp)
    RoutesImpl.create[F](postService, commentService, tagService, pageService, healthService)
  }

  private def mkRoutesWithNoPage[F[_]: Concurrent: Raise[*[_], AppErr]]: Routes[F] = {
    val postService = PostServiceMock.create[F]()
    val commentService = CommentServiceMock.create[F]()
    val tagService = TagServiceMock.create[F]()
    val pageService = PageServiceMock.create[F]()

    val healthService = HealthServiceMock.create[F](timestamp = testTimestamp)
    RoutesImpl.create[F](postService, commentService, tagService, pageService, healthService)
  }

  private def mkRoutesWithPagesList[F[_]: Concurrent: Raise[*[_], AppErr]]: Routes[F] = {
    val postService = PostServiceMock.create[F]()
    val commentService = CommentServiceMock.create[F]()
    val tagService = TagServiceMock.create[F]()
    val pages = ListItemsResult(
      List(
        ListPageResult("about", "About Us"),
        ListPageResult("contact", "Contact")
      ),
      2
    )
    val pageService = PageServiceMock.create[F](getAllPagesResult = pages)

    val healthService = HealthServiceMock.create[F](timestamp = testTimestamp)
    RoutesImpl.create[F](postService, commentService, tagService, pageService, healthService)
  }

  private def mkRoutesWithEmptyPagesList[F[_]: Concurrent: Raise[*[_], AppErr]]: Routes[F] = {
    val postService = PostServiceMock.create[F]()
    val commentService = CommentServiceMock.create[F]()
    val tagService = TagServiceMock.create[F]()
    val pageService = PageServiceMock.create[F](getAllPagesResult = ListItemsResult(Nil, 0))

    val healthService = HealthServiceMock.create[F](timestamp = testTimestamp)
    RoutesImpl.create[F](postService, commentService, tagService, pageService, healthService)
  }

  private def mkRoutesWithHealth[F[_]: Concurrent: Raise[*[_], AppErr]]: Routes[F] = {
    val postService = PostServiceMock.create[F]()
    val commentService = CommentServiceMock.create[F]()
    val tagService = TagServiceMock.create[F]()
    val pageService = PageServiceMock.create[F]()
    val healthService = HealthServiceMock.create[F](timestamp = testTimestamp)

    RoutesImpl.create[F](postService, commentService, tagService, pageService, healthService)
  }

  private def mkRoutesWithUnhealthyDb[F[_]: Concurrent: Raise[*[_], AppErr]]: Routes[F] = {
    val postService = PostServiceMock.create[F]()
    val commentService = CommentServiceMock.create[F]()
    val tagService = TagServiceMock.create[F]()
    val pageService = PageServiceMock.create[F]()
    val healthService =
      HealthServiceMock.create[F](status = "degraded", database = "unhealthy", timestamp = testTimestamp)

    RoutesImpl.create[F](postService, commentService, tagService, pageService, healthService)
  }
}
