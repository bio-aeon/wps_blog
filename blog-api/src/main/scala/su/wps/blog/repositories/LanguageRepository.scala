package su.wps.blog.repositories

import su.wps.blog.models.domain.Language

trait LanguageRepository[DB[_]] {
  def findActive: DB[List[Language]]
  def findDefault: DB[Option[Language]]
  def findByCode(code: String): DB[Option[Language]]
}
