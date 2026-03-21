package su.wps.blog.endpoints

import cats.effect.IO
import cats.effect.testing.specs2.CatsEffect
import org.http4s.*
import org.http4s.headers.`X-Forwarded-For`
import org.http4s.implicits.*
import org.specs2.mutable.Specification
import org.typelevel.ci.CIString

class RateLimitMiddlewareSpec extends Specification with CatsEffect {

  "RateLimitMiddleware" >> {
    "POST requests" >> {
      "allows requests within limit" >> {
        val app = mkRateLimitedApp(maxRequests = 3, windowSeconds = 60)
        runPost(app).map { resp =>
          resp.status mustEqual Status.Ok
        }
      }

      "returns 429 when limit exceeded" >> {
        val app = mkRateLimitedApp(maxRequests = 2, windowSeconds = 60)
        for {
          _ <- runPost(app)
          _ <- runPost(app)
          resp <- runPost(app)
        } yield resp.status mustEqual Status.TooManyRequests
      }

      "includes X-RateLimit-Limit header" >> {
        val app = mkRateLimitedApp(maxRequests = 10, windowSeconds = 60)
        runPost(app).map { resp =>
          headerValue(resp, "X-RateLimit-Limit") must beSome("10")
        }
      }

      "includes X-RateLimit-Remaining header" >> {
        val app = mkRateLimitedApp(maxRequests = 5, windowSeconds = 60)
        runPost(app).map { resp =>
          headerValue(resp, "X-RateLimit-Remaining") must beSome("4")
        }
      }

      "decrements remaining count on successive requests" >> {
        val app = mkRateLimitedApp(maxRequests = 3, windowSeconds = 60)
        for {
          _ <- runPost(app)
          resp <- runPost(app)
        } yield headerValue(resp, "X-RateLimit-Remaining") must beSome("1")
      }

      "includes Retry-After header on 429" >> {
        val app = mkRateLimitedApp(maxRequests = 1, windowSeconds = 30)
        for {
          _ <- runPost(app)
          resp <- runPost(app)
        } yield headerValue(resp, "Retry-After") must beSome("30")
      }

      "sets remaining to 0 on 429" >> {
        val app = mkRateLimitedApp(maxRequests = 1, windowSeconds = 60)
        for {
          _ <- runPost(app)
          resp <- runPost(app)
        } yield headerValue(resp, "X-RateLimit-Remaining") must beSome("0")
      }

      "tracks limits per IP independently" >> {
        val app = mkRateLimitedApp(maxRequests = 1, windowSeconds = 60)
        for {
          _ <- runPostWithIp(app, "1.2.3.4")
          resp <- runPostWithIp(app, "5.6.7.8")
        } yield resp.status mustEqual Status.Ok
      }
    }

    "GET requests" >> {
      "are never rate-limited" >> {
        val app = mkRateLimitedApp(maxRequests = 1, windowSeconds = 60)
        for {
          _ <- runPost(app)
          _ <- runPost(app)
          resp <- runGet(app)
        } yield resp.status mustEqual Status.Ok
      }

      "do not include rate-limit headers" >> {
        val app = mkRateLimitedApp(maxRequests = 5, windowSeconds = 60)
        runGet(app).map { resp =>
          headerValue(resp, "X-RateLimit-Limit") must beNone
        }
      }
    }

    "OPTIONS requests" >> {
      "are never rate-limited" >> {
        val app = mkRateLimitedApp(maxRequests = 1, windowSeconds = 60)
        for {
          _ <- runPost(app)
          _ <- runPost(app)
          resp <- app.run(Request[IO](Method.OPTIONS, uri"/test"))
        } yield resp.status mustEqual Status.Ok
      }
    }

    "HEAD requests" >> {
      "are never rate-limited" >> {
        val app = mkRateLimitedApp(maxRequests = 1, windowSeconds = 60)
        for {
          _ <- runPost(app)
          _ <- runPost(app)
          resp <- app.run(Request[IO](Method.HEAD, uri"/test"))
        } yield resp.status mustEqual Status.Ok
      }
    }
  }

  private val testApp: HttpApp[IO] =
    HttpApp[IO](_ => IO.pure(Response[IO](Status.Ok).withEntity("ok")))

  private def mkRateLimitedApp(maxRequests: Int, windowSeconds: Long): HttpApp[IO] = {
    val fn: HttpApp[IO] => HttpApp[IO] = RateLimitMiddleware[IO](maxRequests, windowSeconds)
    fn(testApp)
  }

  private def runPost(app: HttpApp[IO]): IO[Response[IO]] =
    app.run(Request[IO](Method.POST, uri"/test"))

  private def runGet(app: HttpApp[IO]): IO[Response[IO]] =
    app.run(Request[IO](Method.GET, uri"/test"))

  private def runPostWithIp(app: HttpApp[IO], ip: String): IO[Response[IO]] =
    app
      .run(
        Request[IO](Method.POST, uri"/test")
          .putHeaders(Header.Raw(CIString("X-Forwarded-For"), ip))
      )

  private def headerValue(resp: Response[IO], name: String): Option[String] =
    resp.headers.get(CIString(name)).map(_.head.value)
}
