package su.wps.blog.repositories

import su.wps.blog.models.domain.{Comment, CommentId, PostId}

trait CommentRepository[DB[_]] {
  def insert(comment: Comment): DB[Comment]

  def findCommentsByPostId(postId: PostId): DB[List[Comment]]

  def hasRated(commentId: CommentId, ip: String): DB[Boolean]

  def insertRater(commentId: CommentId, ip: String): DB[Int]

  def updateRating(commentId: CommentId, delta: Int): DB[Int]
}
