package su.wps.blog.repositories

import su.wps.blog.models.domain.Config

trait ConfigRepository[DB[_]] {
  def findByName(name: String): DB[Option[Config]]
  def findByNames(names: List[String]): DB[List[Config]]
}
