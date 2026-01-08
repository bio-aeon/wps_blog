package su.wps.blog.endpoints

import cats.Monad
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.http4s.*
import org.http4s.implicits.*
import org.specs2.mutable.Specification
import su.wps.blog.endpoints.mocks.PostServiceMock
import su.wps.blog.models.api.{ListPostResult, PostResult, TagResult}
import su.wps.blog.models.domain.{AppErr, PostId, TagId}
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
        uri"posts/search".withQueryParams(Map("q" -> "nonexistent", "limit" -> "10", "offset" -> "0"))
      )

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      val respBody = resp.as[String].unsafeRunSync()

      resp.status mustEqual Status.Ok
      respBody mustEqual """{"items":[],"total":0}"""
    }
  }

  private def mkRoutes[F[_]: Monad: Raise[*[_], AppErr]]: Routes[F] = {
    val tags = List(TagResult(TagId(1), "scala", "scala"))
    val postService = PostServiceMock.create[F](
      List(
        ListPostResult(PostId(1), "name", "text", ZonedDateTime.parse("2001-01-01T09:15:00Z"), tags)
      ),
      Some(PostResult("name", "text", ZonedDateTime.parse("2001-01-01T09:15:00Z"), tags))
    )

    RoutesImpl.create[F](postService)
  }

  private def mkRoutesWithTagFilter[F[_]: Monad: Raise[*[_], AppErr]]: Routes[F] = {
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

    RoutesImpl.create[F](postService)
  }

  private def mkRoutesWithSearch[F[_]: Monad: Raise[*[_], AppErr]]: Routes[F] = {
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

    RoutesImpl.create[F](postService)
  }

  private def mkRoutesWithEmptySearch[F[_]: Monad: Raise[*[_], AppErr]]: Routes[F] = {
    val postService = PostServiceMock.create[F](searchPostsResult = Nil)

    RoutesImpl.create[F](postService)
  }
}
