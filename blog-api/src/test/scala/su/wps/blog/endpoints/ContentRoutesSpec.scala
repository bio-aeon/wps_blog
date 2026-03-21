package su.wps.blog.endpoints

import cats.effect.IO
import cats.effect.testing.specs2.CatsEffect
import org.http4s.*
import org.http4s.implicits.*
import org.specs2.mutable.Specification

class ContentRoutesSpec extends Specification with RoutesSpecSupport with CatsEffect {

  "Content routes" >> {
    "GET /tags" >> {
      "returns tags list" >> {
        val routes = buildRoutes[IO](tagsResult = testTagsWithCounts)
        val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/tags"))

        routes.routes.run(request).value.map(_.get).map { resp =>
          resp.status mustEqual Status.Ok
        }
      }

      "returns tags with post counts" >> {
        val routes = buildRoutes[IO](tagsResult = testTagsWithCounts)
        val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/tags"))

        for {
          resp <- routes.routes.run(request).value.map(_.get)
          respBody <- resp.as[String]
        } yield {
          respBody must contain("\"post_count\":")
          respBody must contain("\"total\":2")
        }
      }

      "returns empty list when no tags exist" >> {
        val routes = buildRoutes[IO]()
        val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/tags"))

        for {
          resp <- routes.routes.run(request).value.map(_.get)
          respBody <- resp.as[String]
        } yield {
          resp.status mustEqual Status.Ok
          respBody mustEqual """{"items":[],"total":0}"""
        }
      }
    }

    "GET /tags/cloud" >> {
      "returns tag cloud" >> {
        val routes = buildRoutes[IO](tagCloudResult = testTagCloud)
        val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/tags/cloud"))

        routes.routes.run(request).value.map(_.get).map { resp =>
          resp.status mustEqual Status.Ok
        }
      }

      "returns normalized weights" >> {
        val routes = buildRoutes[IO](tagCloudResult = testTagCloud)
        val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/tags/cloud"))

        for {
          resp <- routes.routes.run(request).value.map(_.get)
          respBody <- resp.as[String]
        } yield {
          respBody must contain("\"weight\":")
          respBody must contain("\"count\":")
          respBody must contain("\"tags\":")
        }
      }

      "returns empty result when no tags exist" >> {
        val routes = buildRoutes[IO]()
        val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/tags/cloud"))

        for {
          resp <- routes.routes.run(request).value.map(_.get)
          respBody <- resp.as[String]
        } yield {
          resp.status mustEqual Status.Ok
          respBody mustEqual """{"tags":[]}"""
        }
      }
    }

    "GET /pages/{url}" >> {
      "returns page content" >> {
        val routes = buildRoutes[IO](pageResult = Some(testPage))
        val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/pages/about"))

        routes.routes.run(request).value.map(_.get).map { resp =>
          resp.status mustEqual Status.Ok
        }
      }

      "returns page with correct fields" >> {
        val routes = buildRoutes[IO](pageResult = Some(testPage))
        val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/pages/about"))

        for {
          resp <- routes.routes.run(request).value.map(_.get)
          respBody <- resp.as[String]
        } yield {
          respBody must contain("\"url\":\"about\"")
          respBody must contain("\"title\":\"About Us\"")
          respBody must contain("\"content\":\"About page content\"")
        }
      }

      "returns 404 when page not found" >> {
        val routes = buildRoutes[IO]()
        val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/pages/non-existent"))

        for {
          resp <- ErrorHandler(routes.routes).run(request).value.map(_.get)
          body <- resp.as[String]
        } yield {
          resp.status mustEqual Status.NotFound
          body must contain("\"code\":\"NOT_FOUND\"")
          body must contain("Page not found")
        }
      }
    }

    "GET /pages" >> {
      "returns pages list" >> {
        val routes = buildRoutes[IO](pagesResult = testPagesList)
        val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/pages"))

        routes.routes.run(request).value.map(_.get).map { resp =>
          resp.status mustEqual Status.Ok
        }
      }

      "returns pages with url and title" >> {
        val routes = buildRoutes[IO](pagesResult = testPagesList)
        val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/pages"))

        for {
          resp <- routes.routes.run(request).value.map(_.get)
          respBody <- resp.as[String]
        } yield {
          respBody must contain("\"url\":\"about\"")
          respBody must contain("\"title\":\"About Us\"")
          respBody must contain("\"total\":2")
        }
      }

      "returns empty list when no pages exist" >> {
        val routes = buildRoutes[IO]()
        val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/pages"))

        for {
          resp <- routes.routes.run(request).value.map(_.get)
          respBody <- resp.as[String]
        } yield {
          resp.status mustEqual Status.Ok
          respBody mustEqual """{"items":[],"total":0}"""
        }
      }
    }
  }

}
