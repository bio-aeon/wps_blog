package su.wps.blog.models.domain

final case class Language(
  code: String,
  name: String,
  nativeName: String,
  isDefault: Boolean,
  isActive: Boolean,
  sortOrder: Int
)
