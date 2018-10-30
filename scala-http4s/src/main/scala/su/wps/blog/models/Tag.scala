package su.wps.blog.models

case class Tag(name: String, slug: String, id: Option[TagId] = None)

case class TagId(value: Int) extends AnyVal
