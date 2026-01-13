package su.wps.blog.repositories.sql

import derevo.derive
import su.wps.blog.models.domain.{Comment, CommentId, PostId}
import tofu.higherKind.derived.representableK

@derive(representableK)
trait CommentSql[DB[_]] {
  def insert(comment: Comment): DB[Comment]

  def findById(commentId: CommentId): DB[Option[Comment]]

  def findCommentsByPostId(postId: PostId): DB[List[Comment]]

  def hasRated(commentId: CommentId, ip: String): DB[Boolean]

  def insertRater(commentId: CommentId, ip: String): DB[Int]

  def updateRating(commentId: CommentId, delta: Int): DB[Int]

  def delete(commentId: CommentId): DB[Int]

  def approve(commentId: CommentId): DB[Int]
}
