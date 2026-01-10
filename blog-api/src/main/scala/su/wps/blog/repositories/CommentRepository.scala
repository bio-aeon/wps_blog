package su.wps.blog.repositories

import su.wps.blog.models.domain.{Comment, PostId}

trait CommentRepository[DB[_]] {
  def insert(comment: Comment): DB[Comment]

  def findCommentsByPostId(postId: PostId): DB[List[Comment]]
}
