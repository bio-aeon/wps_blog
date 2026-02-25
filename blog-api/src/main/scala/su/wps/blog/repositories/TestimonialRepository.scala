package su.wps.blog.repositories

import su.wps.blog.models.domain.Testimonial

trait TestimonialRepository[DB[_]] {
  def findAllActive: DB[List[Testimonial]]
}
