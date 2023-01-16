package su.wps.blog.repositories.sql

import derevo.derive
import su.wps.blog.models.Post
import tofu.higherKind.derived.representableK

@derive(representableK)
trait PostSql[DB[_]] {
  def findAllWithLimitAndOffset(limit: Int, offset: Int): DB[List[Post]]
}
