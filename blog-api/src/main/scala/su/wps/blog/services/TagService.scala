package su.wps.blog.services

import su.wps.blog.models.api.{ListItemsResult, TagCloudResult, TagWithCountResult}

trait TagService[F[_]] {
  def getAllTags(lang: String): F[ListItemsResult[TagWithCountResult]]
  def getTagCloud(lang: String): F[TagCloudResult]
}
