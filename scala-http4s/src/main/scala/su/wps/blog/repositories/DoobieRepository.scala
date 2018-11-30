package su.wps.blog.repositories

import java.sql.Timestamp
import java.time.{ZoneOffset, ZonedDateTime}

import doobie.util.meta.Meta

abstract class DoobieRepository {
  implicit val zonedDateTimeMeta: Meta[ZonedDateTime] =
    Meta[Timestamp].xmap(
      timestamp => ZonedDateTime.ofInstant(timestamp.toInstant, ZoneOffset.UTC),
      zonedDateTime => Timestamp.from(zonedDateTime.toInstant)
    )

}
