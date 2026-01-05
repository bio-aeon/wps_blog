package su.wps.blog.models.domain

import io.circe.Encoder

final case class Tag(name: String, slug: String, id: Option[TagId] = None) {
  def nonEmptyId: TagId = id.getOrElse(throw new IllegalStateException("Empty tag id"))
}

final case class TagId(value: Int) extends AnyVal

object TagId {
  implicit val encoder: Encoder[TagId] = Encoder[Int].contramap(_.value)
}
