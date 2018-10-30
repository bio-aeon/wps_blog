package su.wps.blog.models

import java.time.ZonedDateTime

case class Comment(text: String,
                   name: String,
                   email: String,
                   postId: Int,
                   left: Int,
                   right: Int,
                   treeId: Int,
                   level: Int,
                   rating: Int,
                   createdAt: ZonedDateTime,
                   parentId: Option[Int] = None,
                   id: Option[CommentId] = None)

case class CommentId(value: Int) extends AnyVal
