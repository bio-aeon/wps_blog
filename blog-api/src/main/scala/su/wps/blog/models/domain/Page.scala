package su.wps.blog.models.domain

import java.time.ZonedDateTime

final case class Page(
  url: String,
  title: String,
  content: String,
  createdAt: ZonedDateTime,
  id: Option[PageId] = None
) {
  def nonEmptyId: PageId = id.getOrElse(throw new IllegalStateException("Empty page id"))
}

final case class PageId(value: Int) extends AnyVal
