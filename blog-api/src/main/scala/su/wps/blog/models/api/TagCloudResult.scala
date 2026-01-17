package su.wps.blog.models.api

import io.circe.Encoder

final case class TagCloudItem(name: String, slug: String, count: Int, weight: Double)

object TagCloudItem {
  implicit val encoder: Encoder[TagCloudItem] =
    Encoder.forProduct4("name", "slug", "count", "weight")(TagCloudItem.unapply(_).get)
}

final case class TagCloudResult(tags: List[TagCloudItem])

object TagCloudResult {
  implicit val encoder: Encoder[TagCloudResult] =
    Encoder.forProduct1("tags")(_.tags)
}
