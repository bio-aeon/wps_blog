package su.wps.blog.endpoints

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.http4s.*
import org.http4s.implicits.*
import org.specs2.mutable.Specification
import org.typelevel.ci.CIString

class SecurityHeadersMiddlewareSpec extends Specification {

  private val testApp: HttpApp[IO] = HttpApp[IO](_ => IO.pure(Response[IO](Status.Ok)))
  private val app = SecurityHeadersMiddleware(testApp)

  "SecurityHeadersMiddleware" >> {
    "X-Content-Type-Options" >> {
      "sets nosniff on every response" >> {
        val resp = runRequest(Request[IO](Method.GET, uri"/test"))
        headerValue(resp, "X-Content-Type-Options") must beSome("nosniff")
      }
    }

    "X-Frame-Options" >> {
      "sets DENY on every response" >> {
        val resp = runRequest(Request[IO](Method.GET, uri"/test"))
        headerValue(resp, "X-Frame-Options") must beSome("DENY")
      }
    }

    "Strict-Transport-Security" >> {
      "sets HSTS with max-age and includeSubDomains" >> {
        val resp = runRequest(Request[IO](Method.GET, uri"/test"))
        val hsts = headerValue(resp, "Strict-Transport-Security")
        hsts must beSome[String].which(_.contains("max-age=31536000"))
        hsts must beSome[String].which(_.contains("includeSubDomains"))
      }
    }

    "Referrer-Policy" >> {
      "sets strict-origin-when-cross-origin" >> {
        val resp = runRequest(Request[IO](Method.GET, uri"/test"))
        headerValue(resp, "Referrer-Policy") must beSome("strict-origin-when-cross-origin")
      }
    }

    "Content-Security-Policy" >> {
      "sets restrictive CSP for API responses" >> {
        val resp = runRequest(Request[IO](Method.GET, uri"/test"))
        val csp = headerValue(resp, "Content-Security-Policy")
        csp must beSome[String].which(_.contains("default-src 'none'"))
        csp must beSome[String].which(_.contains("frame-ancestors 'none'"))
      }
    }

    "Permissions-Policy" >> {
      "disables camera, microphone, geolocation" >> {
        val resp = runRequest(Request[IO](Method.GET, uri"/test"))
        val pp = headerValue(resp, "Permissions-Policy")
        pp must beSome[String].which(_.contains("camera=()"))
        pp must beSome[String].which(_.contains("microphone=()"))
        pp must beSome[String].which(_.contains("geolocation=()"))
      }
    }

    "preserves existing response status and body" >> {
      val resp = runRequest(Request[IO](Method.GET, uri"/test"))
      resp.status mustEqual Status.Ok
    }

    "applies headers to POST responses" >> {
      val resp = runRequest(Request[IO](Method.POST, uri"/test"))
      headerValue(resp, "X-Content-Type-Options") must beSome("nosniff")
    }
  }

  private def runRequest(req: Request[IO]): Response[IO] =
    app.run(req).unsafeRunSync()

  private def headerValue(resp: Response[IO], name: String): Option[String] =
    resp.headers.get(CIString(name)).map(_.head.value)
}
