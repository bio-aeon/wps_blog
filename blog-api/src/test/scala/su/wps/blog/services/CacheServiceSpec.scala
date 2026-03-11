package su.wps.blog.services

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.specs2.mutable.Specification

import scala.concurrent.duration.*

class CacheServiceSpec extends Specification {

  "CacheService" >> {
    "getOrLoad" >> {
      "returns loaded value on first call" >> {
        val cache = CacheServiceImpl.create[IO](100)

        val result = cache.getOrLoad("key", 60.seconds)(IO.pure("value")).unsafeRunSync()

        result mustEqual "value"
      }

      "returns cached value on second call without reloading" >> {
        val cache = CacheServiceImpl.create[IO](100)
        var loadCount = 0
        val load = IO { loadCount += 1; "value" }

        cache.getOrLoad("key", 60.seconds)(load).unsafeRunSync()
        cache.getOrLoad("key", 60.seconds)(load).unsafeRunSync()

        loadCount mustEqual 1
      }

      "caches different keys independently" >> {
        val cache = CacheServiceImpl.create[IO](100)
        var loadCount = 0
        def load(v: String) = IO { loadCount += 1; v }

        cache.getOrLoad("key1", 60.seconds)(load("a")).unsafeRunSync()
        cache.getOrLoad("key2", 60.seconds)(load("b")).unsafeRunSync()
        val r1 = cache.getOrLoad("key1", 60.seconds)(load("c")).unsafeRunSync()
        val r2 = cache.getOrLoad("key2", 60.seconds)(load("d")).unsafeRunSync()

        loadCount mustEqual 2
        r1 mustEqual "a"
        r2 mustEqual "b"
      }
    }

    "invalidate" >> {
      "causes next getOrLoad to reload the value" >> {
        val cache = CacheServiceImpl.create[IO](100)
        var loadCount = 0
        val load = IO { loadCount += 1; s"value-$loadCount" }

        cache.getOrLoad("key", 60.seconds)(load).unsafeRunSync()
        cache.invalidate("key").unsafeRunSync()
        val result = cache.getOrLoad("key", 60.seconds)(load).unsafeRunSync()

        loadCount mustEqual 2
        result mustEqual "value-2"
      }

      "does not affect other keys" >> {
        val cache = CacheServiceImpl.create[IO](100)

        cache.getOrLoad("key1", 60.seconds)(IO.pure("a")).unsafeRunSync()
        cache.getOrLoad("key2", 60.seconds)(IO.pure("b")).unsafeRunSync()
        cache.invalidate("key1").unsafeRunSync()

        var reloaded = false
        val r2 = cache.getOrLoad("key2", 60.seconds)(IO { reloaded = true; "c" }).unsafeRunSync()

        reloaded mustEqual false
        r2 mustEqual "b"
      }
    }

    "invalidateAll" >> {
      "clears all cached values" >> {
        val cache = CacheServiceImpl.create[IO](100)
        var loadCount = 0
        def load(v: String) = IO { loadCount += 1; v }

        cache.getOrLoad("key1", 60.seconds)(load("a")).unsafeRunSync()
        cache.getOrLoad("key2", 60.seconds)(load("b")).unsafeRunSync()
        cache.invalidateAll.unsafeRunSync()
        cache.getOrLoad("key1", 60.seconds)(load("c")).unsafeRunSync()
        cache.getOrLoad("key2", 60.seconds)(load("d")).unsafeRunSync()

        loadCount mustEqual 4
      }
    }
  }
}
