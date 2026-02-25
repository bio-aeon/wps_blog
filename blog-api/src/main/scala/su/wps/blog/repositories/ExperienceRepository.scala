package su.wps.blog.repositories

import su.wps.blog.models.domain.Experience

trait ExperienceRepository[DB[_]] {
  def findAllActive: DB[List[Experience]]
}
