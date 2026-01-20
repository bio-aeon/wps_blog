package su.wps.blog.services

import su.wps.blog.models.api.{ListItemsResult, ListPageResult, PageResult}

trait PageService[F[_]] {
  def getPageByUrl(url: String): F[PageResult]
  def getAllPages: F[ListItemsResult[ListPageResult]]
}
