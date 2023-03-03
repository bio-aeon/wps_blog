package su.wps.blog.repositories.sql

import derevo.derive
import su.wps.blog.models.domain.{Comment, PostId}
import tofu.higherKind.derived.representableK

@derive(representableK)
trait CommentSql[DB[_]] {
  def insert(comment: Comment): DB[Comment]

  def findCommentsByPostId(postId: PostId): DB[List[Comment]]
}
