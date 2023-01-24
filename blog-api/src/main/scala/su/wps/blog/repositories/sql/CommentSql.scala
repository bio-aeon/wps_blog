package su.wps.blog.repositories.sql

import derevo.derive
import su.wps.blog.models.{Comment, PostId}
import tofu.higherKind.derived.representableK

@derive(representableK)
trait CommentSql[DB[_]] {
  def findCommentsByPostId(postId: PostId): DB[List[Comment]]
}
