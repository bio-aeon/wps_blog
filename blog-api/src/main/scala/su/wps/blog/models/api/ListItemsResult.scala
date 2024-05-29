package su.wps.blog.models.api

import io.circe.Encoder
import io.circe.generic.semiauto.*

final case class ListItemsResult[T](items: List[T], total: Int)

object ListItemsResult {
  implicit def encoder[T: Encoder]: Encoder[ListItemsResult[T]] = deriveEncoder
}
