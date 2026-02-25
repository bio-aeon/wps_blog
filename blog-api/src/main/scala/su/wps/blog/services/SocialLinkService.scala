package su.wps.blog.services

import su.wps.blog.models.api.SocialLinkResult

trait SocialLinkService[F[_]] {
  def getSocialLinks: F[List[SocialLinkResult]]
}
