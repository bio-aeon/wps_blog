package su.wps.blog.repositories.sql

import derevo.derive
import su.wps.blog.models.domain.SocialLink
import tofu.higherKind.derived.representableK

@derive(representableK)
trait SocialLinkSql[DB[_]] {
  def findAllActive: DB[List[SocialLink]]
}
