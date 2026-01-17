package su.wps.blog.services

import su.wps.blog.models.api.{ListItemsResult, TagWithCountResult}

trait TagService[F[_]] {
  def getAllTags: F[ListItemsResult[TagWithCountResult]]
}
