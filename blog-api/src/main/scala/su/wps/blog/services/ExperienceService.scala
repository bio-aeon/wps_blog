package su.wps.blog.services

import su.wps.blog.models.api.ExperienceResult

trait ExperienceService[F[_]] {
  def getExperiences: F[List[ExperienceResult]]
}
