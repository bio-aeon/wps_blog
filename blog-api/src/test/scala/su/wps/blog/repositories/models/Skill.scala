package su.wps.blog.repositories.models

import cats.syntax.functor.*
import doobie.*
import su.wps.blog.instances.time.*
import su.wps.blog.tools.types.*

import java.time.ZonedDateTime

final case class Skill(
  id: PosInt,
  name: Varchar[W.`100`.T],
  slug: Varchar[W.`100`.T],
  category: Varchar[W.`100`.T],
  proficiency: Int,
  icon: Option[Varchar[W.`255`.T]],
  sortOrder: Int,
  isActive: Boolean,
  createdAt: ZonedDateTime
)

object Skill {

  object sql {
    def insert(skill: Skill): ConnectionIO[Unit] = {
      val sql =
        """
          |INSERT INTO skills (
          | id,
          | name,
          | slug,
          | category,
          | proficiency,
          | icon,
          | sort_order,
          | is_active,
          | created_at
          |) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
          |""".stripMargin
      Update[Skill](sql).run(skill).void
    }
  }
}
