package su.wps.blog.repositories

import su.wps.blog.models.{Comment, PostId}

trait CommentRepository[DB[_]] {
  def findCommentsByPostId(postId: PostId): DB[List[Comment]]
}
