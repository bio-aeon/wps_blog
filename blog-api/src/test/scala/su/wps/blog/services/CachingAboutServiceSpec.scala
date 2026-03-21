package su.wps.blog.services

import cats.effect.IO
import cats.effect.testing.specs2.CatsEffect
import org.specs2.mutable.Specification
import su.wps.blog.models.api.*

import scala.concurrent.duration.*

class CachingAboutServiceSpec extends Specification with CatsEffect {

  private val emptyAbout =
    AboutResult(ProfileResult("", "", "", "", ""), Nil, Nil, Nil)

  "CachingAboutService" >> {
    "getAboutPage" >> {
      "returns cached result on repeated calls" >> {
        var callCount = 0
        val underlying = new AboutService[IO] {
          def getAboutPage: IO[AboutResult] = IO { callCount += 1; emptyAbout }
        }
        val cache = CacheServiceImpl.create[IO](100)
        val cached = CachingAboutService.create[IO](underlying, cache, 60.seconds)

        for {
          _ <- cached.getAboutPage
          _ <- cached.getAboutPage
        } yield callCount mustEqual 1
      }

      "returns fresh result after cache invalidation" >> {
        var callCount = 0
        val underlying = new AboutService[IO] {
          def getAboutPage: IO[AboutResult] = IO { callCount += 1; emptyAbout }
        }
        val cache = CacheServiceImpl.create[IO](100)
        val cached = CachingAboutService.create[IO](underlying, cache, 60.seconds)

        for {
          _ <- cached.getAboutPage
          _ <- cache.invalidate("about:page")
          _ <- cached.getAboutPage
        } yield callCount mustEqual 2
      }
    }
  }
}
