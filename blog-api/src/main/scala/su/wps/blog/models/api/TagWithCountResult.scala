package su.wps.blog.models.api

import io.circe.Encoder
import su.wps.blog.models.domain.TagId

final case class TagWithCountResult(id: TagId, name: String, slug: String, postCount: Int)

object TagWithCountResult {
  implicit val encoder: Encoder[TagWithCountResult] =
    Encoder.forProduct4("id", "name", "slug", "post_count")(TagWithCountResult.unapply(_).get)
}
