package su.wps.blog.services

import cats.effect.IO
import cats.effect.testing.specs2.CatsEffect
import org.specs2.mutable.Specification

import scala.concurrent.duration.*

class CacheServiceSpec extends Specification with CatsEffect {

  "CacheService" >> {
    "getOrLoad" >> {
      "returns loaded value on first call" >> {
        val cache = CacheServiceImpl.create[IO](100)

        cache.getOrLoad("key", 60.seconds)(IO.pure("value")).map { result =>
          result mustEqual "value"
        }
      }

      "returns cached value on second call without reloading" >> {
        val cache = CacheServiceImpl.create[IO](100)
        var loadCount = 0
        val load = IO { loadCount += 1; "value" }

        for {
          _ <- cache.getOrLoad("key", 60.seconds)(load)
          _ <- cache.getOrLoad("key", 60.seconds)(load)
        } yield loadCount mustEqual 1
      }

      "caches different keys independently" >> {
        val cache = CacheServiceImpl.create[IO](100)
        var loadCount = 0
        def load(v: String) = IO { loadCount += 1; v }

        for {
          _ <- cache.getOrLoad("key1", 60.seconds)(load("a"))
          _ <- cache.getOrLoad("key2", 60.seconds)(load("b"))
          r1 <- cache.getOrLoad("key1", 60.seconds)(load("c"))
          r2 <- cache.getOrLoad("key2", 60.seconds)(load("d"))
        } yield {
          loadCount mustEqual 2
          r1 mustEqual "a"
          r2 mustEqual "b"
        }
      }
    }

    "invalidate" >> {
      "causes next getOrLoad to reload the value" >> {
        val cache = CacheServiceImpl.create[IO](100)
        var loadCount = 0
        val load = IO { loadCount += 1; s"value-$loadCount" }

        for {
          _ <- cache.getOrLoad("key", 60.seconds)(load)
          _ <- cache.invalidate("key")
          result <- cache.getOrLoad("key", 60.seconds)(load)
        } yield {
          loadCount mustEqual 2
          result mustEqual "value-2"
        }
      }

      "does not affect other keys" >> {
        val cache = CacheServiceImpl.create[IO](100)

        var reloaded = false
        for {
          _ <- cache.getOrLoad("key1", 60.seconds)(IO.pure("a"))
          _ <- cache.getOrLoad("key2", 60.seconds)(IO.pure("b"))
          _ <- cache.invalidate("key1")
          r2 <- cache.getOrLoad("key2", 60.seconds)(IO { reloaded = true; "c" })
        } yield {
          reloaded mustEqual false
          r2 mustEqual "b"
        }
      }
    }

    "invalidateAll" >> {
      "clears all cached values" >> {
        val cache = CacheServiceImpl.create[IO](100)
        var loadCount = 0
        def load(v: String) = IO { loadCount += 1; v }

        for {
          _ <- cache.getOrLoad("key1", 60.seconds)(load("a"))
          _ <- cache.getOrLoad("key2", 60.seconds)(load("b"))
          _ <- cache.invalidateAll
          _ <- cache.getOrLoad("key1", 60.seconds)(load("c"))
          _ <- cache.getOrLoad("key2", 60.seconds)(load("d"))
        } yield loadCount mustEqual 4
      }
    }
  }
}
