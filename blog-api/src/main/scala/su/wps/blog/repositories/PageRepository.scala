package su.wps.blog.repositories

import su.wps.blog.models.domain.Page

trait PageRepository[DB[_]] {
  def findByUrl(url: String): DB[Option[Page]]
  def findAll: DB[List[Page]]
}
