package su.wps.blog.repositories.sql

import derevo.derive
import su.wps.blog.models.{Post, PostId}
import tofu.higherKind.derived.representableK

@derive(representableK)
trait PostSql[DB[_]] {
  def findAllWithLimitAndOffset(limit: Int, offset: Int): DB[List[Post]]

  def findCount: DB[Int]

  def findById(id: PostId): DB[Option[Post]]
}
