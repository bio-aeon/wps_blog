package su.wps.blog.models.domain

sealed trait AppErr extends Exception {
  override def getMessage: String = toString
}

object AppErr {
  final case class PostNotFound(id: PostId) extends AppErr {
    override def toString: String = s"Post with id $id not found"
  }

  final case class CommentNotFound(id: CommentId) extends AppErr {
    override def toString: String = s"Comment with id $id not found"
  }

  final case class PageNotFound(url: String) extends AppErr {
    override def toString: String = s"Page with url '$url' not found"
  }

  final case class ValidationFailed(errors: Map[String, String]) extends AppErr {
    override def toString: String =
      s"Validation failed: ${errors.map { case (k, v) => s"$k: $v" }.mkString(", ")}"
  }
}
