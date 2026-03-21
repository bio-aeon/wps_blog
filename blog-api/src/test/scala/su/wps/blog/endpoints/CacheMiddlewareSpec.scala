package su.wps.blog.endpoints

import cats.effect.IO
import cats.effect.testing.specs2.CatsEffect
import org.http4s.*
import org.http4s.dsl.io.*
import org.http4s.implicits.*
import org.specs2.mutable.Specification
import org.typelevel.ci.CIString

class CacheMiddlewareSpec extends Specification with CatsEffect {

  "CacheMiddleware" >> {
    "GET /v1/posts" >> {
      "sets public Cache-Control with max-age=60" >> {
        runRequest(Request[IO](Method.GET, uri"/v1/posts")).map { resp =>
          cacheControl(resp) must beSome("public, max-age=60")
        }
      }

      "includes Vary: Accept-Encoding header" >> {
        runRequest(Request[IO](Method.GET, uri"/v1/posts")).map { resp =>
          headerValue(resp, "Vary") must beSome("Accept-Encoding")
        }
      }
    }

    "GET /v1/posts/{id}" >> {
      "sets Cache-Control with max-age=300 for post detail" >> {
        runRequest(Request[IO](Method.GET, uri"/v1/posts/42")).map { resp =>
          cacheControl(resp) must beSome("public, max-age=300")
        }
      }
    }

    "GET /v1/posts/{id}/comments" >> {
      "sets short-lived Cache-Control with max-age=30" >> {
        runRequest(Request[IO](Method.GET, uri"/v1/posts/1/comments")).map { resp =>
          cacheControl(resp) must beSome("public, max-age=30")
        }
      }
    }

    "GET /v1/tags" >> {
      "sets static data Cache-Control with max-age=3600" >> {
        runRequest(Request[IO](Method.GET, uri"/v1/tags")).map { resp =>
          cacheControl(resp) must beSome("public, max-age=3600")
        }
      }
    }

    "GET /v1/about" >> {
      "sets static data Cache-Control with max-age=3600" >> {
        runRequest(Request[IO](Method.GET, uri"/v1/about")).map { resp =>
          cacheControl(resp) must beSome("public, max-age=3600")
        }
      }
    }

    "GET /health" >> {
      "sets no-cache, no-store for health endpoint" >> {
        runRequest(Request[IO](Method.GET, uri"/health")).map { resp =>
          cacheControl(resp) must beSome("no-cache, no-store")
        }
      }
    }

    "POST requests" >> {
      "sets no-cache, no-store for mutation endpoints" >> {
        runRequest(Request[IO](Method.POST, uri"/v1/posts/1/view")).map { resp =>
          cacheControl(resp) must beSome("no-cache, no-store")
        }
      }
    }

    "passes through response body unchanged" >> {
      for {
        resp <- runRequest(Request[IO](Method.GET, uri"/v1/tags"))
        body <- resp.as[String]
      } yield body mustEqual "ok"
    }
  }

  private val testRoutes: HttpRoutes[IO] = HttpRoutes.of[IO] { case _ =>
    IO.pure(Response[IO](Status.Ok).withEntity("ok"))
  }

  private def runRequest(request: Request[IO]): IO[Response[IO]] =
    CacheMiddleware(testRoutes).run(request).value.map(_.get)

  private def cacheControl(resp: Response[IO]): Option[String] =
    headerValue(resp, "Cache-Control")

  private def headerValue(resp: Response[IO], name: String): Option[String] =
    resp.headers.get(CIString(name)).map(_.head.value)
}
