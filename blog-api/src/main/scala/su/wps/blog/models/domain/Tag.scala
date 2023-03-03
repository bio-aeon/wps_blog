package su.wps.blog.models.domain

final case class Tag(name: String, slug: String, id: Option[TagId] = None)

final case class TagId(value: Int) extends AnyVal
