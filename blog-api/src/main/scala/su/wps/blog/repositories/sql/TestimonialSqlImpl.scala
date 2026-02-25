package su.wps.blog.repositories.sql

import cats.tagless.syntax.functorK.*
import doobie.*
import doobie.implicits.*
import su.wps.blog.instances.time.*
import su.wps.blog.models.domain.Testimonial
import tofu.doobie.LiftConnectionIO

final class TestimonialSqlImpl private extends TestimonialSql[ConnectionIO] {

  def findAllActive: ConnectionIO[List[Testimonial]] =
    sql"""SELECT author_name, author_title, author_company, author_url,
                 author_image_url, quote, sort_order, is_active, created_at, id
          FROM testimonials
          WHERE is_active = true
          ORDER BY sort_order, id"""
      .query[Testimonial]
      .to[List]
}

object TestimonialSqlImpl {
  def create[DB[_]](implicit L: LiftConnectionIO[DB]): TestimonialSql[DB] =
    new TestimonialSqlImpl().mapK(L.liftF)
}
