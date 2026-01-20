package su.wps.blog.services

import su.wps.blog.models.api.PageResult

trait PageService[F[_]] {
  def getPageByUrl(url: String): F[PageResult]
}
