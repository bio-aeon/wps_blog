package su.wps.blog.services

import cats.effect.Sync
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import com.github.benmanes.caffeine.cache.{Caffeine, Cache => JCache}

import scala.concurrent.duration.FiniteDuration

final class CacheServiceImpl[F[_]] private (
  underlying: JCache[String, Any]
)(implicit F: Sync[F])
    extends CacheService[F] {

  def getOrLoad[A](key: String, ttl: FiniteDuration)(load: F[A]): F[A] =
    F.delay(Option(underlying.getIfPresent(key).asInstanceOf[A])).flatMap {
      case Some(cached) => F.pure(cached)
      case None =>
        load.flatMap { value =>
          F.delay(underlying.put(key, value.asInstanceOf[Any])).as(value)
        }
    }

  def invalidate(key: String): F[Unit] = F.delay(underlying.invalidate(key))

  def invalidateAll: F[Unit] = F.delay(underlying.invalidateAll())
}

object CacheServiceImpl {

  def create[F[_]: Sync](maxEntries: Long): CacheServiceImpl[F] = {
    val underlying: JCache[String, Any] = Caffeine
      .newBuilder()
      .maximumSize(maxEntries)
      .build()

    new CacheServiceImpl[F](underlying)
  }
}
