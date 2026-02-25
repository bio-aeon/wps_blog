package su.wps.blog.services

import su.wps.blog.models.api.TestimonialResult

trait TestimonialService[F[_]] {
  def getTestimonials: F[List[TestimonialResult]]
}
