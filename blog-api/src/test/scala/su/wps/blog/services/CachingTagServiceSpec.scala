package su.wps.blog.services

import cats.effect.IO
import cats.effect.testing.specs2.CatsEffect
import org.specs2.mutable.Specification
import su.wps.blog.models.api.*

import scala.concurrent.duration.*

class CachingTagServiceSpec extends Specification with CatsEffect {

  "CachingTagService" >> {
    "getAllTags" >> {
      "returns cached result on repeated calls" >> {
        var callCount = 0
        val underlying = countingTagService { () =>
          callCount += 1; callCount
        }
        val cache = CacheServiceImpl.create[IO](100)
        val cached = CachingTagService.create[IO](underlying, cache, 60.seconds)

        for {
          _ <- cached.getAllTags("en")
          _ <- cached.getAllTags("en")
        } yield callCount mustEqual 1
      }

      "returns fresh result after cache invalidation" >> {
        var callCount = 0
        val underlying = countingTagService { () =>
          callCount += 1; callCount
        }
        val cache = CacheServiceImpl.create[IO](100)
        val cached = CachingTagService.create[IO](underlying, cache, 60.seconds)

        for {
          _ <- cached.getAllTags("en")
          _ <- cache.invalidate("tags:all:en")
          _ <- cached.getAllTags("en")
        } yield callCount mustEqual 2
      }
    }

    "getTagCloud" >> {
      "caches tag cloud independently from tag list" >> {
        var listCalls = 0
        var cloudCalls = 0
        val underlying = new TagService[IO] {
          def getAllTags(lang: String): IO[ListItemsResult[TagWithCountResult]] =
            IO { listCalls += 1; ListItemsResult(Nil, 0) }

          def getTagCloud(lang: String): IO[TagCloudResult] =
            IO { cloudCalls += 1; TagCloudResult(Nil) }
        }
        val cache = CacheServiceImpl.create[IO](100)
        val cached = CachingTagService.create[IO](underlying, cache, 60.seconds)

        for {
          _ <- cached.getAllTags("en")
          _ <- cached.getTagCloud("en")
          _ <- cached.getAllTags("en")
          _ <- cached.getTagCloud("en")
        } yield {
          listCalls mustEqual 1
          cloudCalls mustEqual 1
        }
      }
    }
  }

  private def countingTagService(counter: () => Int): TagService[IO] =
    new TagService[IO] {
      def getAllTags(lang: String): IO[ListItemsResult[TagWithCountResult]] =
        IO { counter(); ListItemsResult(Nil, 0) }

      def getTagCloud(lang: String): IO[TagCloudResult] =
        IO { counter(); TagCloudResult(Nil) }
    }
}
