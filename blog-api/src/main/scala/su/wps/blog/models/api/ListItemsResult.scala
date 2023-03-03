package su.wps.blog.models.api

final case class ListItemsResult[T](items: List[T], total: Int)
