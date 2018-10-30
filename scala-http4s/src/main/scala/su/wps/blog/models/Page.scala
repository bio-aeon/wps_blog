package su.wps.blog.models

import java.time.ZonedDateTime

case class Page(url: String,
                title: String,
                content: String,
                createdAt: ZonedDateTime,
                id: Option[PageId] = None)

case class PageId(value: Int) extends AnyVal
