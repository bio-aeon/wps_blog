package su.wps.blog.services

import su.wps.blog.models.api.{ListItemsResult, ListPageResult, PageResult}

trait PageService[F[_]] {
  def getPageByUrl(lang: String, url: String): F[PageResult]
  def getAllPages(lang: String): F[ListItemsResult[ListPageResult]]
}
