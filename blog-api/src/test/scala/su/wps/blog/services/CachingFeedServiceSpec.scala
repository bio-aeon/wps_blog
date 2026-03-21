package su.wps.blog.services

import cats.effect.IO
import cats.effect.testing.specs2.CatsEffect
import org.specs2.mutable.Specification
import su.wps.blog.models.api.FeedResult

import scala.concurrent.duration.*

class CachingFeedServiceSpec extends Specification with CatsEffect {

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

        for {
          _ <- cached.getFeed("en")
          _ <- cached.getFeed("en")
        } yield callCount mustEqual 1
      }

      "returns fresh result after cache invalidation" >> {
        var callCount = 0
        val underlying = new FeedService[IO] {
          def getFeed(lang: String): IO[FeedResult] = IO { callCount += 1; emptyFeed }
        }
        val cache = CacheServiceImpl.create[IO](100)
        val cached = CachingFeedService.create[IO](underlying, cache, 60.seconds)

        for {
          _ <- cached.getFeed("en")
          _ <- cache.invalidate("feed:all:en")
          _ <- cached.getFeed("en")
        } yield callCount mustEqual 2
      }
    }
  }
}
