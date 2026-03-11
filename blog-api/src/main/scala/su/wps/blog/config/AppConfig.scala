package su.wps.blog.config

import com.comcast.ip4s.{Ipv4Address, Port}
import pureconfig.ConfigReader
import pureconfig.error.CannotConvert
import pureconfig.generic.auto.exportReader

final case class AppConfig(db: DbConfig, httpServer: HttpServerConfig, cache: CacheConfig)

final case class DbConfig(
  driver: String,
  url: String,
  username: String,
  password: String,
  pool: PoolConfig
)

final case class PoolConfig(
  maximumPoolSize: Int,
  minimumIdle: Int,
  idleTimeoutMs: Long,
  maxLifetimeMs: Long,
  connectionTimeoutMs: Long,
  leakDetectionThresholdMs: Long
)

final case class HttpServerConfig(interface: Ipv4Address, port: Port)

final case class CacheConfig(
  tagsTtlSeconds: Int,
  aboutTtlSeconds: Int,
  feedTtlSeconds: Int,
  maxEntries: Long
)

object AppConfig {
  implicit val ipv4AddressReader: ConfigReader[Ipv4Address] = ConfigReader[String].emap(x =>
    Ipv4Address.fromString(x).toRight(CannotConvert(x, "Ipv4Address", "Incorrect interface"))
  )

  implicit val portReader: ConfigReader[Port] =
    ConfigReader[Int].emap(x =>
      Port.fromInt(x).toRight(CannotConvert(x.toString, "Port", "Incorrect port"))
    )

  implicit val reader: ConfigReader[AppConfig] = exportReader[AppConfig].instance
}
