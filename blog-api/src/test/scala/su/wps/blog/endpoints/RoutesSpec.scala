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

  "Api routes" >> {
    "GET /posts returns paginated post list" >> {
      val routes =
        buildRoutes[IO](allPostsResult = testPostList, postByIdResult = Some(testSinglePost))
      val request =
        Request[IO](
          Method.GET,
          Uri.unsafeFromString(s"$v1/posts").withQueryParams(Map("limit" -> "10", "offset" -> "0"))
        )

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      val respBody = resp.as[String].unsafeRunSync()

      resp.status mustEqual Status.Ok
      respBody mustEqual """{"items":[{"id":1,"name":"name","short_text":"text","created_at":"2001-01-01T09:15:00Z","tags":[{"id":1,"name":"scala","slug":"scala"}]}],"total":1}"""
    }

    "GET /posts/{id} returns post by id" >> {
      val routes =
        buildRoutes[IO](allPostsResult = testPostList, postByIdResult = Some(testSinglePost))
      val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/posts/1"))

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      val respBody = resp.as[String].unsafeRunSync()

      resp.status mustEqual Status.Ok
      respBody mustEqual """{"name":"name","text":"text","created_at":"2001-01-01T09:15:00Z","tags":[{"id":1,"name":"scala","slug":"scala"}]}"""
    }

    "returns 200 with filtered posts when tag parameter is provided" >> {
      val routes =
        buildRoutes[IO](allPostsResult = testPostList, postsByTagResult = testTaggedPosts)
      val request = Request[IO](
        Method.GET,
        Uri
          .unsafeFromString(s"$v1/posts")
          .withQueryParams(Map("limit" -> "10", "offset" -> "0", "tag" -> "scala"))
      )

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      resp.status mustEqual Status.Ok
    }

    "returns posts tagged with the specified tag" >> {
      val routes =
        buildRoutes[IO](allPostsResult = testPostList, postsByTagResult = testTaggedPosts)
      val request = Request[IO](
        Method.GET,
        Uri
          .unsafeFromString(s"$v1/posts")
          .withQueryParams(Map("limit" -> "10", "offset" -> "0", "tag" -> "scala"))
      )

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      val respBody = resp.as[String].unsafeRunSync()

      respBody must contain("\"slug\":\"scala\"")
    }

    "returns all posts when tag parameter is not provided" >> {
      val routes =
        buildRoutes[IO](allPostsResult = testPostList, postsByTagResult = testTaggedPosts)
      val request =
        Request[IO](
          Method.GET,
          Uri.unsafeFromString(s"$v1/posts").withQueryParams(Map("limit" -> "10", "offset" -> "0"))
        )

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      val respBody = resp.as[String].unsafeRunSync()

      resp.status mustEqual Status.Ok
      respBody must contain("\"total\":1")
    }

    "returns 204 No Content on successful view increment" >> {
      val routes = buildRoutes[IO]()
      val request = Request[IO](Method.POST, Uri.unsafeFromString(s"$v1/posts/1/view"))

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()

      resp.status mustEqual Status.NoContent
    }

    "returns 204 even for non-existent post (idempotent)" >> {
      val routes = buildRoutes[IO]()
      val request = Request[IO](Method.POST, Uri.unsafeFromString(s"$v1/posts/99999/view"))

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()

      resp.status mustEqual Status.NoContent
    }

    "returns 200 with search results for GET /posts/search" >> {
      val routes = buildRoutes[IO](searchPostsResult = testSearchResults)
      val request = Request[IO](
        Method.GET,
        Uri
          .unsafeFromString(s"$v1/posts/search")
          .withQueryParams(Map("q" -> "scala", "limit" -> "10", "offset" -> "0"))
      )

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      resp.status mustEqual Status.Ok
    }

    "returns matching posts in search response" >> {
      val routes = buildRoutes[IO](searchPostsResult = testSearchResults)
      val request = Request[IO](
        Method.GET,
        Uri
          .unsafeFromString(s"$v1/posts/search")
          .withQueryParams(Map("q" -> "scala", "limit" -> "10", "offset" -> "0"))
      )

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      val respBody = resp.as[String].unsafeRunSync()

      respBody must contain("scala-tutorial")
      respBody must contain("\"total\":2")
    }

    "returns empty list when search has no matches" >> {
      val routes = buildRoutes[IO]()
      val request = Request[IO](
        Method.GET,
        Uri
          .unsafeFromString(s"$v1/posts/search")
          .withQueryParams(Map("q" -> "nonexistent", "limit" -> "10", "offset" -> "0"))
      )

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      val respBody = resp.as[String].unsafeRunSync()

      resp.status mustEqual Status.Ok
      respBody mustEqual """{"items":[],"total":0}"""
    }

    "returns 200 with recent posts for GET /posts/recent" >> {
      val routes = buildRoutes[IO](recentPostsResult = testRecentPosts)
      val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/posts/recent"))

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      resp.status mustEqual Status.Ok
    }

    "returns recent posts with default count when count param is not provided" >> {
      val routes = buildRoutes[IO](recentPostsResult = testRecentPosts)
      val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/posts/recent"))

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      val respBody = resp.as[String].unsafeRunSync()

      resp.status mustEqual Status.Ok
      respBody must contain("recent-post")
    }

    "returns recent posts with specified count" >> {
      val routes = buildRoutes[IO](recentPostsResult = testRecentPosts)
      val request = Request[IO](
        Method.GET,
        Uri.unsafeFromString(s"$v1/posts/recent").withQueryParams(Map("count" -> "3"))
      )

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      resp.status mustEqual Status.Ok
    }

    "returns empty list when no recent posts exist" >> {
      val routes = buildRoutes[IO]()
      val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/posts/recent"))

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      val respBody = resp.as[String].unsafeRunSync()

      resp.status mustEqual Status.Ok
      respBody mustEqual "[]"
    }

    "returns 200 with threaded comments for GET /posts/{id}/comments" >> {
      val routes = buildRoutes[IO](commentsResult = testCommentsResult)
      val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/posts/1/comments"))

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()

      resp.status mustEqual Status.Ok
    }

    "returns JSON with comments array and total" >> {
      val routes = buildRoutes[IO](commentsResult = testCommentsResult)
      val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/posts/1/comments"))

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      val respBody = resp.as[String].unsafeRunSync()

      respBody must contain("\"comments\":")
      respBody must contain("\"total\":2")
    }

    "returns nested replies structure in comments" >> {
      val routes = buildRoutes[IO](commentsResult = testCommentsResult)
      val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/posts/1/comments"))

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      val respBody = resp.as[String].unsafeRunSync()

      respBody must contain("\"replies\":")
      respBody must contain("Reply text")
    }

    "returns empty comments for post with no comments" >> {
      val routes = buildRoutes[IO]()
      val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/posts/99999/comments"))

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      val respBody = resp.as[String].unsafeRunSync()

      resp.status mustEqual Status.Ok
      respBody mustEqual """{"comments":[],"total":0}"""
    }

    "returns 201 Created when creating a new comment" >> {
      val routes = buildRoutes[IO](createCommentResult = Some(testCreatedComment))
      val body = CreateCommentRequest("Author", "test@example.com", "Comment text", None)
      val request = Request[IO](Method.POST, Uri.unsafeFromString(s"$v1/posts/1/comments"))
        .withEntity(body.asJson)

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()

      resp.status mustEqual Status.Created
    }

    "returns created comment in response body" >> {
      val routes = buildRoutes[IO](createCommentResult = Some(testCreatedComment))
      val body = CreateCommentRequest("Author", "test@example.com", "Comment text", None)
      val request = Request[IO](Method.POST, Uri.unsafeFromString(s"$v1/posts/1/comments"))
        .withEntity(body.asJson)

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      val respBody = resp.as[String].unsafeRunSync()

      respBody must contain("\"name\":\"Author\"")
      respBody must contain("\"text\":\"Comment text\"")
    }

    "returns comment with id in response" >> {
      val routes = buildRoutes[IO](createCommentResult = Some(testCreatedComment))
      val body = CreateCommentRequest("Author", "test@example.com", "Comment text", None)
      val request = Request[IO](Method.POST, Uri.unsafeFromString(s"$v1/posts/1/comments"))
        .withEntity(body.asJson)

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      val respBody = resp.as[String].unsafeRunSync()

      respBody must contain("\"id\":")
    }

    "creates reply comment with parent id" >> {
      val routes = buildRoutes[IO](createCommentResult = Some(testCreatedComment))
      val body = CreateCommentRequest("Replier", "reply@example.com", "Reply text", Some(1))
      val request = Request[IO](Method.POST, Uri.unsafeFromString(s"$v1/posts/1/comments"))
        .withEntity(body.asJson)

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()

      resp.status mustEqual Status.Created
    }

    "returns 204 No Content on successful comment rating" >> {
      val routes = buildRoutes[IO]()
      val body = RateCommentRequest(isUpvote = true)
      val request = Request[IO](Method.POST, Uri.unsafeFromString(s"$v1/comments/1/rate"))
        .withEntity(body.asJson)

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()

      resp.status mustEqual Status.NoContent
    }

    "handles upvote rating request" >> {
      val routes = buildRoutes[IO]()
      val body = RateCommentRequest(isUpvote = true)
      val request = Request[IO](Method.POST, Uri.unsafeFromString(s"$v1/comments/1/rate"))
        .withEntity(body.asJson)

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()

      resp.status mustEqual Status.NoContent
    }

    "handles downvote rating request" >> {
      val routes = buildRoutes[IO]()
      val body = RateCommentRequest(isUpvote = false)
      val request = Request[IO](Method.POST, Uri.unsafeFromString(s"$v1/comments/1/rate"))
        .withEntity(body.asJson)

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()

      resp.status mustEqual Status.NoContent
    }

    "returns 204 even for non-existent comment (idempotent)" >> {
      val routes = buildRoutes[IO]()
      val body = RateCommentRequest(isUpvote = true)
      val request = Request[IO](Method.POST, Uri.unsafeFromString(s"$v1/comments/99999/rate"))
        .withEntity(body.asJson)

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()

      resp.status mustEqual Status.NoContent
    }

    "returns 200 with tags list for GET /tags" >> {
      val routes = buildRoutes[IO](tagsResult = testTagsWithCounts)
      val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/tags"))

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()

      resp.status mustEqual Status.Ok
    }

    "returns tags with post counts in response" >> {
      val routes = buildRoutes[IO](tagsResult = testTagsWithCounts)
      val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/tags"))

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      val respBody = resp.as[String].unsafeRunSync()

      respBody must contain("\"post_count\":")
      respBody must contain("\"total\":2")
    }

    "returns empty tags list when no tags exist" >> {
      val routes = buildRoutes[IO]()
      val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/tags"))

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      val respBody = resp.as[String].unsafeRunSync()

      resp.status mustEqual Status.Ok
      respBody mustEqual """{"items":[],"total":0}"""
    }

    "returns 200 with tag cloud for GET /tags/cloud" >> {
      val routes = buildRoutes[IO](tagCloudResult = testTagCloud)
      val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/tags/cloud"))

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()

      resp.status mustEqual Status.Ok
    }

    "returns tag cloud with normalized weights" >> {
      val routes = buildRoutes[IO](tagCloudResult = testTagCloud)
      val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/tags/cloud"))

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      val respBody = resp.as[String].unsafeRunSync()

      respBody must contain("\"weight\":")
      respBody must contain("\"count\":")
      respBody must contain("\"tags\":")
    }

    "returns empty tag cloud when no tags exist" >> {
      val routes = buildRoutes[IO]()
      val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/tags/cloud"))

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      val respBody = resp.as[String].unsafeRunSync()

      resp.status mustEqual Status.Ok
      respBody mustEqual """{"tags":[]}"""
    }

    "returns 200 with page content for GET /pages/{url}" >> {
      val routes = buildRoutes[IO](pageResult = Some(testPage))
      val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/pages/about"))

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()

      resp.status mustEqual Status.Ok
    }

    "returns page with correct fields in response" >> {
      val routes = buildRoutes[IO](pageResult = Some(testPage))
      val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/pages/about"))

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      val respBody = resp.as[String].unsafeRunSync()

      respBody must contain("\"url\":\"about\"")
      respBody must contain("\"title\":\"About Us\"")
      respBody must contain("\"content\":\"About page content\"")
    }

    "returns 404 with error response when page is not found" >> {
      val routes = buildRoutes[IO]()
      val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/pages/non-existent"))

      val resp =
        ErrorHandler(routes.routes).run(request).value.map(_.get).unsafeRunSync()

      resp.status mustEqual Status.NotFound
      val body = resp.as[String].unsafeRunSync()
      body must contain("\"code\":\"NOT_FOUND\"")
      body must contain("Page not found")
    }

    "returns 200 with pages list for GET /pages" >> {
      val routes = buildRoutes[IO](pagesResult = testPagesList)
      val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/pages"))

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()

      resp.status mustEqual Status.Ok
    }

    "returns pages with url and title in response" >> {
      val routes = buildRoutes[IO](pagesResult = testPagesList)
      val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/pages"))

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      val respBody = resp.as[String].unsafeRunSync()

      respBody must contain("\"url\":\"about\"")
      respBody must contain("\"title\":\"About Us\"")
      respBody must contain("\"total\":2")
    }

    "returns empty pages list when no pages exist" >> {
      val routes = buildRoutes[IO]()
      val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/pages"))

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      val respBody = resp.as[String].unsafeRunSync()

      resp.status mustEqual Status.Ok
      respBody mustEqual """{"items":[],"total":0}"""
    }

    "returns 200 with health status for GET /health" >> {
      val routes = buildRoutes[IO]()
      val request = Request[IO](Method.GET, uri"health")

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()

      resp.status mustEqual Status.Ok
    }

    "returns health response with status, database and timestamp fields" >> {
      val routes = buildRoutes[IO]()
      val request = Request[IO](Method.GET, uri"health")

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      val respBody = resp.as[String].unsafeRunSync()

      respBody must contain("\"status\":\"healthy\"")
      respBody must contain("\"database\":\"healthy\"")
      respBody must contain("\"timestamp\":")
    }

    "returns degraded status when database is unhealthy" >> {
      val routes = buildRoutes[IO](healthStatus = "degraded", healthDatabase = "unhealthy")
      val request = Request[IO](Method.GET, uri"health")

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      val respBody = resp.as[String].unsafeRunSync()

      resp.status mustEqual Status.Ok
      respBody must contain("\"status\":\"degraded\"")
      respBody must contain("\"database\":\"unhealthy\"")
    }

    "returns 400 for invalid pagination limit on GET /posts" >> {
      val routes = buildRoutes[IO]()
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

    "returns 400 for negative pagination offset on GET /posts" >> {
      val routes = buildRoutes[IO]()
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

    "returns 400 for limit exceeding maximum on GET /posts" >> {
      val routes = buildRoutes[IO]()
      val request = Request[IO](
        Method.GET,
        Uri.unsafeFromString(s"$v1/posts").withQueryParams(Map("limit" -> "101", "offset" -> "0"))
      )

      val resp =
        ErrorHandler(routes.routes).run(request).value.map(_.get).unsafeRunSync()

      resp.status mustEqual Status.BadRequest
    }

    "returns 400 for invalid pagination on GET /posts/search" >> {
      val routes = buildRoutes[IO](searchPostsResult = testSearchResults)
      val request = Request[IO](
        Method.GET,
        Uri
          .unsafeFromString(s"$v1/posts/search")
          .withQueryParams(Map("q" -> "scala", "limit" -> "-1", "offset" -> "0"))
      )

      val resp =
        ErrorHandler(routes.routes).run(request).value.map(_.get).unsafeRunSync()

      resp.status mustEqual Status.BadRequest
    }

    "returns 400 for empty comment name on POST /posts/{id}/comments" >> {
      val routes = buildRoutes[IO](createCommentResult = Some(testCreatedComment))
      val body = CreateCommentRequest("", "test@example.com", "Comment text", None)
      val request = Request[IO](Method.POST, Uri.unsafeFromString(s"$v1/posts/1/comments"))
        .withEntity(body.asJson)

      val resp =
        ErrorHandler(routes.routes).run(request).value.map(_.get).unsafeRunSync()

      resp.status mustEqual Status.BadRequest
      val respBody = resp.as[String].unsafeRunSync()
      respBody must contain("\"code\":\"VALIDATION_ERROR\"")
      respBody must contain("\"name\"")
    }

    "returns 400 for invalid email on POST /posts/{id}/comments" >> {
      val routes = buildRoutes[IO](createCommentResult = Some(testCreatedComment))
      val body = CreateCommentRequest("Author", "not-an-email", "Comment text", None)
      val request = Request[IO](Method.POST, Uri.unsafeFromString(s"$v1/posts/1/comments"))
        .withEntity(body.asJson)

      val resp =
        ErrorHandler(routes.routes).run(request).value.map(_.get).unsafeRunSync()

      resp.status mustEqual Status.BadRequest
      val respBody = resp.as[String].unsafeRunSync()
      respBody must contain("\"email\"")
    }

    "returns 400 for empty comment text on POST /posts/{id}/comments" >> {
      val routes = buildRoutes[IO](createCommentResult = Some(testCreatedComment))
      val body = CreateCommentRequest("Author", "test@example.com", "", None)
      val request = Request[IO](Method.POST, Uri.unsafeFromString(s"$v1/posts/1/comments"))
        .withEntity(body.asJson)

      val resp =
        ErrorHandler(routes.routes).run(request).value.map(_.get).unsafeRunSync()

      resp.status mustEqual Status.BadRequest
      val respBody = resp.as[String].unsafeRunSync()
      respBody must contain("\"text\"")
    }

    "sanitizes HTML in comment text on POST /posts/{id}/comments" >> {
      val routes = buildRoutes[IO]()
      val body =
        CreateCommentRequest("Author", "test@example.com", "<script>alert('xss')</script>", None)
      val request = Request[IO](Method.POST, Uri.unsafeFromString(s"$v1/posts/1/comments"))
        .withEntity(body.asJson)

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()

      resp.status mustEqual Status.Created
      val respBody = resp.as[String].unsafeRunSync()
      respBody must not contain "<script>"
    }

    "accumulates multiple validation errors on POST /posts/{id}/comments" >> {
      val routes = buildRoutes[IO](createCommentResult = Some(testCreatedComment))
      val body = CreateCommentRequest("", "invalid", "", None)
      val request = Request[IO](Method.POST, Uri.unsafeFromString(s"$v1/posts/1/comments"))
        .withEntity(body.asJson)

      val resp =
        ErrorHandler(routes.routes).run(request).value.map(_.get).unsafeRunSync()

      resp.status mustEqual Status.BadRequest
      val respBody = resp.as[String].unsafeRunSync()
      respBody must contain("\"name\"")
      respBody must contain("\"email\"")
      respBody must contain("\"text\"")
    }

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

    "does not serve API routes without version prefix" >> {
      val routes = buildRoutes[IO]()
      val request =
        Request[IO](Method.GET, uri"posts".withQueryParams(Map("limit" -> "10", "offset" -> "0")))

      val resp = routes.routes.run(request).value.unsafeRunSync()

      resp must beNone
    }

    "serves health endpoint without version prefix" >> {
      val routes = buildRoutes[IO]()
      val request = Request[IO](Method.GET, uri"health")

      val resp = routes.routes.run(request).value.unsafeRunSync()

      resp must beSome
      resp.get.status mustEqual Status.Ok
    }

    "returns 200 with skills grouped by category for GET /skills" >> {
      val routes = buildRoutes[IO](skillsResult = testSkillCategories)
      val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/skills"))

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      val respBody = resp.as[String].unsafeRunSync()

      resp.status mustEqual Status.Ok
      respBody must contain("\"category\":")
      respBody must contain("\"skills\":")
    }

    "returns 200 with experiences for GET /experiences" >> {
      val routes = buildRoutes[IO](experiencesResult = testExperiences)
      val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/experiences"))

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      val respBody = resp.as[String].unsafeRunSync()

      resp.status mustEqual Status.Ok
      respBody must contain("\"company\":")
      respBody must contain("\"position\":")
    }

    "returns 200 with social links for GET /social-links" >> {
      val routes = buildRoutes[IO](socialLinksResult = testSocialLinks)
      val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/social-links"))

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      val respBody = resp.as[String].unsafeRunSync()

      resp.status mustEqual Status.Ok
      respBody must contain("\"platform\":")
      respBody must contain("\"url\":")
    }

    "returns 200 on successful contact submission for POST /contact" >> {
      val routes = buildRoutes[IO]()
      val body =
        CreateContactRequest("John", "john@example.com", "Hello", "Test message body", None)
      val request =
        Request[IO](Method.POST, Uri.unsafeFromString(s"$v1/contact")).withEntity(body.asJson)

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      val respBody = resp.as[String].unsafeRunSync()

      resp.status mustEqual Status.Ok
      respBody must contain("\"message\":")
    }

    "returns 400 on contact validation failure for POST /contact" >> {
      val routes = buildRoutes[IO]()
      val body = CreateContactRequest("", "invalid", "ab", "short", None)
      val request =
        Request[IO](Method.POST, Uri.unsafeFromString(s"$v1/contact")).withEntity(body.asJson)

      val resp =
        ErrorHandler(routes.routes).run(request).value.map(_.get).unsafeRunSync()

      resp.status mustEqual Status.BadRequest
      val respBody = resp.as[String].unsafeRunSync()
      respBody must contain("\"code\":\"VALIDATION_ERROR\"")
    }

    "returns 200 with about page data for GET /about" >> {
      val routes = buildRoutes[IO](aboutResult = testAbout)
      val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/about"))

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      val respBody = resp.as[String].unsafeRunSync()

      resp.status mustEqual Status.Ok
      respBody must contain("\"profile\":")
      respBody must contain("\"skills\":")
      respBody must contain("\"experiences\":")
      respBody must contain("\"social_links\":")
    }
  }

  private val testTags = List(TagResult(TagId(1), "scala", "scala"))

  private val testPostList = List(
    ListPostResult(PostId(1), "name", "text", testTimestamp, testTags)
  )

  private val testSinglePost =
    PostResult("name", "text", testTimestamp, testTags)

  private val testTaggedPosts = List(
    ListPostResult(PostId(2), "scala-post", "scala-text", testTimestamp, testTags)
  )

  private val testSearchResults = List(
    ListPostResult(PostId(1), "scala-tutorial", "Learn Scala programming", testTimestamp, testTags),
    ListPostResult(
      PostId(2),
      "scala-advanced",
      "Advanced Scala topics",
      ZonedDateTime.parse("2001-01-02T09:15:00Z"),
      testTags
    )
  )

  private val testRecentPosts = List(
    ListPostResult(PostId(1), "recent-post", "Recent post text", testTimestamp, testTags),
    ListPostResult(
      PostId(2),
      "another-recent",
      "Another recent post",
      ZonedDateTime.parse("2001-01-02T09:15:00Z"),
      testTags
    )
  )

  private val testCommentsResult = {
    val reply = CommentResult(
      CommentId(2),
      "Replier",
      "Reply text",
      1,
      ZonedDateTime.parse("2001-01-01T10:00:00Z"),
      Nil
    )
    val root = CommentResult(
      CommentId(1),
      "Author",
      "Root comment",
      5,
      ZonedDateTime.parse("2001-01-01T09:00:00Z"),
      List(reply)
    )
    CommentsListResult(List(root), 2)
  }

  private val testCreatedComment = CommentResult(
    CommentId(1),
    "Author",
    "Comment text",
    0,
    ZonedDateTime.parse("2001-01-01T09:00:00Z"),
    Nil
  )

  private val testTagsWithCounts = List(
    TagWithCountResult(TagId(1), "scala", "scala", 10),
    TagWithCountResult(TagId(2), "rust", "rust", 5)
  )

  private val testTagCloud = TagCloudResult(
    List(TagCloudItem("scala", "scala", 10, 1.0), TagCloudItem("rust", "rust", 5, 0.5))
  )

  private val testPage = PageResult(1, "about", "About Us", "About page content", testTimestamp)

  private val testPagesList = ListItemsResult(
    List(ListPageResult("about", "About Us"), ListPageResult("contact", "Contact")),
    2
  )

  private val testSkillCategories = List(
    SkillCategoryResult(
      "Backend",
      List(SkillResult(su.wps.blog.models.domain.SkillId(1), "Scala", "scala", "Backend", 90, None))
    )
  )

  private val testExperiences = List(
    ExperienceResult(
      su.wps.blog.models.domain.ExperienceId(1),
      "Acme Corp",
      "Engineer",
      "Description",
      java.time.LocalDate.of(2020, 1, 1),
      None,
      Some("Remote"),
      None
    )
  )

  private val testSocialLinks = List(
    SocialLinkResult(
      su.wps.blog.models.domain.SocialLinkId(1),
      "github",
      "https://github.com/user",
      Some("GitHub"),
      None
    )
  )

  private val testAbout = AboutResult(
    ProfileResult("John", "Engineer", "/photo.jpg", "/resume.pdf", "Bio text"),
    testSkillCategories,
    List(
      ExperienceResult(
        su.wps.blog.models.domain.ExperienceId(1),
        "Acme",
        "Dev",
        "Desc",
        java.time.LocalDate.of(2020, 1, 1),
        None,
        None,
        None
      )
    ),
    List(
      SocialLinkResult(
        su.wps.blog.models.domain.SocialLinkId(1),
        "github",
        "https://github.com",
        Some("GitHub"),
        None
      )
    )
  )

  private def buildRoutes[F[_]: Concurrent: Raise[*[_], AppErr]](
    allPostsResult: List[ListPostResult] = Nil,
    postByIdResult: Option[PostResult] = None,
    postsByTagResult: List[ListPostResult] = Nil,
    searchPostsResult: List[ListPostResult] = Nil,
    recentPostsResult: List[ListPostResult] = Nil,
    commentsResult: CommentsListResult = CommentsListResult(Nil, 0),
    createCommentResult: Option[CommentResult] = None,
    tagsResult: List[TagWithCountResult] = Nil,
    tagCloudResult: TagCloudResult = TagCloudResult(Nil),
    pageResult: Option[PageResult] = None,
    pagesResult: ListItemsResult[ListPageResult] = ListItemsResult(Nil, 0),
    healthStatus: String = "healthy",
    healthDatabase: String = "healthy",
    skillsResult: List[SkillCategoryResult] = Nil,
    experiencesResult: List[ExperienceResult] = Nil,
    socialLinksResult: List[SocialLinkResult] = Nil,
    aboutResult: AboutResult = AboutResult(ProfileResult("", "", "", "", ""), Nil, Nil, Nil)
  ): Routes[F] = RoutesImpl.create[F](
    PostServiceMock.create[F](
      allPostsResult,
      postByIdResult,
      postsByTagResult,
      searchPostsResult,
      recentPostsResult = recentPostsResult
    ),
    CommentServiceMock.create[F](commentsResult, createCommentResult),
    TagServiceMock.create[F](tagsResult, tagCloudResult),
    PageServiceMock.create[F](pageResult, pagesResult),
    HealthServiceMock.create[F](healthStatus, healthDatabase, testTimestamp),
    SkillServiceMock.create[F](skillsResult),
    ExperienceServiceMock.create[F](experiencesResult),
    SocialLinkServiceMock.create[F](socialLinksResult),
    ContactServiceMock.create[F](),
    AboutServiceMock.create[F](aboutResult)
  )

}
