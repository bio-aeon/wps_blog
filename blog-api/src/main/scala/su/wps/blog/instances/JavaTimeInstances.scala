package su.wps.blog.instances

import doobie.Meta
import doobie.implicits.javasql.TimestampMeta

import java.sql.Timestamp
import java.time.{ZoneId, ZonedDateTime}

trait JavaTimeInstances {

  implicit val zonedDateTimeMeta: Meta[ZonedDateTime] =
    TimestampMeta.imap(_.toInstant.atZone(ZoneId.systemDefault))(dt => Timestamp.from(dt.toInstant))
}
