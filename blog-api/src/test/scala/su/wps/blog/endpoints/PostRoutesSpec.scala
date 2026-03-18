package su.wps.blog.endpoints

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.http4s.*
import org.http4s.implicits.*
import org.specs2.mutable.Specification

class PostRoutesSpec extends Specification with RoutesSpecSupport {

  "POST routes" >> {
    "GET /posts" >> {
      "returns paginated post list" >> {
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
        respBody mustEqual """{"items":[{"id":1,"name":"name","short_text":"text","created_at":"2001-01-01T09:15:00Z","language":"en","tags":[{"id":1,"name":"scala","slug":"scala"}],"available_languages":["en"]}],"total":1}"""
      }

      "returns filtered posts when tag parameter provided" >> {
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

      "returns tagged posts" >> {
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

      "returns all posts when tag parameter not provided" >> {
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
    }

    "GET /posts/{id}" >> {
      "returns post by id" >> {
        val routes =
          buildRoutes[IO](allPostsResult = testPostList, postByIdResult = Some(testSinglePost))
        val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/posts/1"))

        val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
        val respBody = resp.as[String].unsafeRunSync()

        resp.status mustEqual Status.Ok
        respBody mustEqual """{"id":1,"name":"name","text":"text","created_at":"2001-01-01T09:15:00Z","language":"en","tags":[{"id":1,"name":"scala","slug":"scala"}],"seo":null,"available_languages":["en"]}"""
      }
    }

    "POST /posts/{id}/view" >> {
      "returns 204 on success" >> {
        val routes = buildRoutes[IO]()
        val request = Request[IO](Method.POST, Uri.unsafeFromString(s"$v1/posts/1/view"))

        val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()

        resp.status mustEqual Status.NoContent
      }

      "returns 204 for non-existent post (idempotent)" >> {
        val routes = buildRoutes[IO]()
        val request = Request[IO](Method.POST, Uri.unsafeFromString(s"$v1/posts/99999/view"))

        val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()

        resp.status mustEqual Status.NoContent
      }
    }

    "GET /posts/search" >> {
      "returns search results" >> {
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

      "returns matching posts" >> {
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

      "returns empty list when no matches" >> {
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
    }

    "GET /posts/recent" >> {
      "returns recent posts" >> {
        val routes = buildRoutes[IO](recentPostsResult = testRecentPosts)
        val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/posts/recent"))

        val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
        resp.status mustEqual Status.Ok
      }

      "uses default count when param not provided" >> {
        val routes = buildRoutes[IO](recentPostsResult = testRecentPosts)
        val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/posts/recent"))

        val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
        val respBody = resp.as[String].unsafeRunSync()

        resp.status mustEqual Status.Ok
        respBody must contain("recent-post")
      }

      "respects specified count" >> {
        val routes = buildRoutes[IO](recentPostsResult = testRecentPosts)
        val request = Request[IO](
          Method.GET,
          Uri.unsafeFromString(s"$v1/posts/recent").withQueryParams(Map("count" -> "3"))
        )

        val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
        resp.status mustEqual Status.Ok
      }

      "returns empty list when no posts exist" >> {
        val routes = buildRoutes[IO]()
        val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/posts/recent"))

        val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
        val respBody = resp.as[String].unsafeRunSync()

        resp.status mustEqual Status.Ok
        respBody mustEqual "[]"
      }
    }
  }

}
