package su.wps.blog.services

import io.scalaland.chimney.dsl.*
import su.wps.blog.models.api.TagResult
import su.wps.blog.models.domain.Tag

private[services] object ServiceHelpers {
  def nonEmpty(s: String): Option[String] =
    Option(s).map(_.trim).filter(_.nonEmpty)

  def tagsToTagResults(tags: List[Tag]): List[TagResult] =
    tags.map(t => t.into[TagResult].withFieldComputed(_.id, _.nonEmptyId).transform)
}
