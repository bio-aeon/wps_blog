package su.wps.blog.services

import scala.concurrent.duration.FiniteDuration

trait CacheService[F[_]] {
  def getOrLoad[A](key: String, ttl: FiniteDuration)(load: F[A]): F[A]
  def invalidate(key: String): F[Unit]
  def invalidateAll: F[Unit]
}
