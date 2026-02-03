package su.wps.blog.services

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.specs2.mutable.Specification

class HealthServiceSpec extends Specification {

  "HealthService should" >> {
    "return healthy status when database check succeeds" >> {
      val service = HealthServiceImpl.create[IO](IO.pure(true))
      val result = service.check.unsafeRunSync()

      result.status mustEqual "healthy"
      result.database mustEqual "healthy"
      result.timestamp must not(beEmpty)
    }

    "return degraded status when database check fails" >> {
      val service = HealthServiceImpl.create[IO](IO.pure(false))
      val result = service.check.unsafeRunSync()

      result.status mustEqual "degraded"
      result.database mustEqual "unhealthy"
    }

    "produce ISO-8601 timestamp" >> {
      val service = HealthServiceImpl.create[IO](IO.pure(true))
      val result = service.check.unsafeRunSync()

      result.timestamp must beMatching("""\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}.*Z""")
    }
  }
}
