package su.wps.blog.endpoints

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.http4s.*
import org.http4s.implicits.*
import org.specs2.mutable.Specification
import org.typelevel.ci.CIString

class CorrelationIdMiddlewareSpec extends Specification {

  private val RequestIdHeader = CIString("X-Request-Id")

  private val testApp: HttpApp[IO] = HttpApp[IO](_ => IO.pure(Response[IO](Status.Ok)))

  private val app = CorrelationIdMiddleware(testApp)

  "CorrelationIdMiddleware" >> {
    "generates X-Request-Id when not present in request" >> {
      val resp = app.run(Request[IO](Method.GET, uri"/test")).unsafeRunSync()
      resp.headers.get(RequestIdHeader) must beSome
    }

    "preserves X-Request-Id from incoming request" >> {
      val customId = "test-correlation-id-123"
      val req = Request[IO](Method.GET, uri"/test")
        .putHeaders(Header.Raw(RequestIdHeader, customId))
      val resp = app.run(req).unsafeRunSync()
      resp.headers.get(RequestIdHeader).map(_.head.value) must beSome(customId)
    }

    "adds X-Request-Id to response headers" >> {
      val resp = app.run(Request[IO](Method.GET, uri"/any")).unsafeRunSync()
      val id = resp.headers.get(RequestIdHeader).map(_.head.value)
      id must beSome[String].which(_.nonEmpty)
    }
  }
}
