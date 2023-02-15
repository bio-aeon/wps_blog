package su.wps.blog.models

final case class CommentRater(ip: String, commentId: CommentId, id: Option[Long] = None)
