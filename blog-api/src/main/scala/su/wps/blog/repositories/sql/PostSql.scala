package su.wps.blog.repositories.sql

import derevo.derive
import su.wps.blog.models.domain.{Post, PostId}
import tofu.higherKind.derived.representableK

@derive(representableK)
trait PostSql[DB[_]] {
  def findAllWithLimitAndOffset(limit: Int, offset: Int): DB[List[Post]]

  def findCount: DB[Int]

  def findById(id: PostId): DB[Option[Post]]

  // Admin methods - include hidden posts
  def findAllWithLimitAndOffsetIncludeHidden(limit: Int, offset: Int): DB[List[Post]]

  def findCountIncludeHidden: DB[Int]
}
