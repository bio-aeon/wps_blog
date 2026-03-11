package su.wps.blog.endpoints

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.http4s.*
import org.http4s.dsl.io.*
import org.http4s.implicits.*
import org.specs2.mutable.Specification
import org.typelevel.ci.CIString

class CacheMiddlewareSpec extends Specification {

  "CacheMiddleware" >> {
    "GET /v1/posts" >> {
      "sets public Cache-Control with max-age=60" >> {
        val resp = runRequest(Request[IO](Method.GET, uri"/v1/posts"))
        cacheControl(resp) must beSome("public, max-age=60")
      }

      "includes Vary: Accept-Encoding header" >> {
        val resp = runRequest(Request[IO](Method.GET, uri"/v1/posts"))
        headerValue(resp, "Vary") must beSome("Accept-Encoding")
      }
    }

    "GET /v1/posts/{id}" >> {
      "sets Cache-Control with max-age=300 for post detail" >> {
        val resp = runRequest(Request[IO](Method.GET, uri"/v1/posts/42"))
        cacheControl(resp) must beSome("public, max-age=300")
      }
    }

    "GET /v1/posts/{id}/comments" >> {
      "sets short-lived Cache-Control with max-age=30" >> {
        val resp = runRequest(Request[IO](Method.GET, uri"/v1/posts/1/comments"))
        cacheControl(resp) must beSome("public, max-age=30")
      }
    }

    "GET /v1/tags" >> {
      "sets static data Cache-Control with max-age=3600" >> {
        val resp = runRequest(Request[IO](Method.GET, uri"/v1/tags"))
        cacheControl(resp) must beSome("public, max-age=3600")
      }
    }

    "GET /v1/about" >> {
      "sets static data Cache-Control with max-age=3600" >> {
        val resp = runRequest(Request[IO](Method.GET, uri"/v1/about"))
        cacheControl(resp) must beSome("public, max-age=3600")
      }
    }

    "GET /health" >> {
      "sets no-cache, no-store for health endpoint" >> {
        val resp = runRequest(Request[IO](Method.GET, uri"/health"))
        cacheControl(resp) must beSome("no-cache, no-store")
      }
    }

    "POST requests" >> {
      "sets no-cache, no-store for mutation endpoints" >> {
        val resp = runRequest(Request[IO](Method.POST, uri"/v1/posts/1/view"))
        cacheControl(resp) must beSome("no-cache, no-store")
      }
    }

    "passes through response body unchanged" >> {
      val resp = runRequest(Request[IO](Method.GET, uri"/v1/tags"))
      resp.as[String].unsafeRunSync() mustEqual "ok"
    }
  }

  private val testRoutes: HttpRoutes[IO] = HttpRoutes.of[IO] { case _ =>
    IO.pure(Response[IO](Status.Ok).withEntity("ok"))
  }

  private def runRequest(request: Request[IO]): Response[IO] =
    CacheMiddleware(testRoutes).run(request).value.map(_.get).unsafeRunSync()

  private def cacheControl(resp: Response[IO]): Option[String] =
    headerValue(resp, "Cache-Control")

  private def headerValue(resp: Response[IO], name: String): Option[String] =
    resp.headers.get(CIString(name)).map(_.head.value)
}
