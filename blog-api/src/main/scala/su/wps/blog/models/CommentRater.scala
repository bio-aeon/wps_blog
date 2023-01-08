package su.wps.blog.models

case class CommentRater(ip: String, commentId: CommentId, id: Option[Long] = None)
