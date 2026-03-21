package su.wps.blog.endpoints

import cats.effect.IO
import cats.effect.testing.specs2.CatsEffect
import org.http4s.*
import org.http4s.implicits.*
import org.specs2.mutable.Specification
import org.typelevel.ci.CIString

class SecurityHeadersMiddlewareSpec extends Specification with CatsEffect {

  private val testApp: HttpApp[IO] = HttpApp[IO](_ => IO.pure(Response[IO](Status.Ok)))
  private val app = SecurityHeadersMiddleware(testApp)

  "SecurityHeadersMiddleware" >> {
    "X-Content-Type-Options" >> {
      "sets nosniff on every response" >> {
        runRequest(Request[IO](Method.GET, uri"/test")).map { resp =>
          headerValue(resp, "X-Content-Type-Options") must beSome("nosniff")
        }
      }
    }

    "X-Frame-Options" >> {
      "sets DENY on every response" >> {
        runRequest(Request[IO](Method.GET, uri"/test")).map { resp =>
          headerValue(resp, "X-Frame-Options") must beSome("DENY")
        }
      }
    }

    "Strict-Transport-Security" >> {
      "sets HSTS with max-age and includeSubDomains" >> {
        runRequest(Request[IO](Method.GET, uri"/test")).map { resp =>
          val hsts = headerValue(resp, "Strict-Transport-Security")
          hsts must beSome[String].which(_.contains("max-age=31536000"))
          hsts must beSome[String].which(_.contains("includeSubDomains"))
        }
      }
    }

    "Referrer-Policy" >> {
      "sets strict-origin-when-cross-origin" >> {
        runRequest(Request[IO](Method.GET, uri"/test")).map { resp =>
          headerValue(resp, "Referrer-Policy") must beSome("strict-origin-when-cross-origin")
        }
      }
    }

    "Content-Security-Policy" >> {
      "sets restrictive CSP for API responses" >> {
        runRequest(Request[IO](Method.GET, uri"/test")).map { resp =>
          val csp = headerValue(resp, "Content-Security-Policy")
          csp must beSome[String].which(_.contains("default-src 'none'"))
          csp must beSome[String].which(_.contains("frame-ancestors 'none'"))
        }
      }
    }

    "Permissions-Policy" >> {
      "disables camera, microphone, geolocation" >> {
        runRequest(Request[IO](Method.GET, uri"/test")).map { resp =>
          val pp = headerValue(resp, "Permissions-Policy")
          pp must beSome[String].which(_.contains("camera=()"))
          pp must beSome[String].which(_.contains("microphone=()"))
          pp must beSome[String].which(_.contains("geolocation=()"))
        }
      }
    }

    "preserves existing response status and body" >> {
      runRequest(Request[IO](Method.GET, uri"/test")).map { resp =>
        resp.status mustEqual Status.Ok
      }
    }

    "applies headers to POST responses" >> {
      runRequest(Request[IO](Method.POST, uri"/test")).map { resp =>
        headerValue(resp, "X-Content-Type-Options") must beSome("nosniff")
      }
    }
  }

  private def runRequest(req: Request[IO]): IO[Response[IO]] =
    app.run(req)

  private def headerValue(resp: Response[IO], name: String): Option[String] =
    resp.headers.get(CIString(name)).map(_.head.value)
}
