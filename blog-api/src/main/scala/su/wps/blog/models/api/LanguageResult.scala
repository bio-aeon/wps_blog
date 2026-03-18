package su.wps.blog.models.api

import io.circe.Encoder

final case class LanguageResult(
  code: String,
  name: String,
  nativeName: String,
  isDefault: Boolean
)

object LanguageResult {
  implicit val encoder: Encoder[LanguageResult] =
    Encoder.forProduct4("code", "name", "native_name", "is_default")(
      LanguageResult.unapply(_).get
    )
}
