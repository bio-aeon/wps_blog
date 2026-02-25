package su.wps.blog.repositories

import su.wps.blog.models.domain.SocialLink

trait SocialLinkRepository[DB[_]] {
  def findAllActive: DB[List[SocialLink]]
}
