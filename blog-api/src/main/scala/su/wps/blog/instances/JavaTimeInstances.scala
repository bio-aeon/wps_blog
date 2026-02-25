package su.wps.blog.instances

import doobie.Meta
import doobie.implicits.javasql.{DateMeta, TimestampMeta}

import java.sql.Timestamp
import java.time.{LocalDate, ZoneId, ZonedDateTime}

trait JavaTimeInstances {

  implicit val zonedDateTimeMeta: Meta[ZonedDateTime] =
    TimestampMeta.imap(_.toInstant.atZone(ZoneId.systemDefault))(dt => Timestamp.from(dt.toInstant))

  implicit val localDateMeta: Meta[LocalDate] =
    DateMeta.imap(_.toLocalDate)(java.sql.Date.valueOf)
}
