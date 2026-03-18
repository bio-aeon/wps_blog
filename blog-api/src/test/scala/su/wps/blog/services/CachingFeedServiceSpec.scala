package su.wps.blog.services

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.specs2.mutable.Specification
import su.wps.blog.models.api.FeedResult

import scala.concurrent.duration.*

class CachingFeedServiceSpec extends Specification {

  private val emptyFeed = FeedResult(Nil, Nil, Nil)

  "CachingFeedService" >> {
    "getFeed" >> {
      "returns cached result on repeated calls" >> {
        var callCount = 0
        val underlying = new FeedService[IO] {
          def getFeed(lang: String): IO[FeedResult] = IO { callCount += 1; emptyFeed }
        }
        val cache = CacheServiceImpl.create[IO](100)
        val cached = CachingFeedService.create[IO](underlying, cache, 60.seconds)

        cached.getFeed("en").unsafeRunSync()
        cached.getFeed("en").unsafeRunSync()

        callCount mustEqual 1
      }

      "returns fresh result after cache invalidation" >> {
        var callCount = 0
        val underlying = new FeedService[IO] {
          def getFeed(lang: String): IO[FeedResult] = IO { callCount += 1; emptyFeed }
        }
        val cache = CacheServiceImpl.create[IO](100)
        val cached = CachingFeedService.create[IO](underlying, cache, 60.seconds)

        cached.getFeed("en").unsafeRunSync()
        cache.invalidate("feed:all:en").unsafeRunSync()
        cached.getFeed("en").unsafeRunSync()

        callCount mustEqual 2
      }
    }
  }
}
