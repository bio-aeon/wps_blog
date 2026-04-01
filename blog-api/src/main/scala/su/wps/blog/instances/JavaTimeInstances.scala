package su.wps.blog.instances

import doobie.Meta
import doobie.implicits.javatimedrivernative.{JavaInstantMeta, JavaLocalDateMeta}

import java.time.{LocalDate, ZoneId, ZonedDateTime}

trait JavaTimeInstances {

  implicit val zonedDateTimeMeta: Meta[ZonedDateTime] =
    JavaInstantMeta.imap(_.atZone(ZoneId.systemDefault))(_.toInstant)

  implicit val localDateMeta: Meta[LocalDate] = JavaLocalDateMeta
}
