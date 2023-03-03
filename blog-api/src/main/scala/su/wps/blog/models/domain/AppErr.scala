package su.wps.blog.models.domain

sealed trait AppErr extends Exception {
  override def getMessage: String = toString
}

object AppErr {
  final case class PostNotFound(id: PostId) extends AppErr {
    override def toString: String = s"Post with id $id not found"
  }
}
