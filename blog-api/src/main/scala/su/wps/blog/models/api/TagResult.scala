package su.wps.blog.models.api

import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder
import su.wps.blog.models.domain.TagId

final case class TagResult(id: TagId, name: String, slug: String)

object TagResult {
  implicit val encoder: Encoder[TagResult] = deriveEncoder[TagResult]
}
