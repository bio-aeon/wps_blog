package su.wps.blog.services

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.specs2.mutable.Specification
import su.wps.blog.models.api.*

import scala.concurrent.duration.*

class CachingAboutServiceSpec extends Specification {

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

        cached.getAboutPage.unsafeRunSync()
        cached.getAboutPage.unsafeRunSync()

        callCount mustEqual 1
      }

      "returns fresh result after cache invalidation" >> {
        var callCount = 0
        val underlying = new AboutService[IO] {
          def getAboutPage: IO[AboutResult] = IO { callCount += 1; emptyAbout }
        }
        val cache = CacheServiceImpl.create[IO](100)
        val cached = CachingAboutService.create[IO](underlying, cache, 60.seconds)

        cached.getAboutPage.unsafeRunSync()
        cache.invalidate("about:page").unsafeRunSync()
        cached.getAboutPage.unsafeRunSync()

        callCount mustEqual 2
      }
    }
  }
}
