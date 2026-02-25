package su.wps.blog.services

import su.wps.blog.models.api.AboutResult

trait AboutService[F[_]] {
  def getAboutPage: F[AboutResult]
}
