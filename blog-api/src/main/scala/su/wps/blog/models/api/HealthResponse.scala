package su.wps.blog.models.api

import io.circe.Encoder

final case class HealthResponse(
  status: String,
  database: String,
  timestamp: String
)

object HealthResponse {
  implicit val encoder: Encoder[HealthResponse] =
    Encoder.forProduct3("status", "database", "timestamp")(h =>
      (h.status, h.database, h.timestamp)
    )
}
