package su.wps.blog.models.domain

final case class CommentRater(ip: String, commentId: CommentId, id: Option[Long] = None)
