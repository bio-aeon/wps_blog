package su.wps.blog.repositories

import su.wps.blog.models.domain.{Comment, PostId}

trait CommentRepository[DB[_]] {
  def findCommentsByPostId(postId: PostId): DB[List[Comment]]
}
