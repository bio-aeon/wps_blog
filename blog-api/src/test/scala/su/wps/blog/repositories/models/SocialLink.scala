package su.wps.blog.repositories.models

import cats.syntax.functor.*
import doobie.*
import doobie.generic.auto.*
import su.wps.blog.instances.time.*
import su.wps.blog.tools.types.*

import java.time.ZonedDateTime

final case class SocialLink(
  id: PosInt,
  platform: Varchar[W.`50`.T],
  url: Varchar[W.`500`.T],
  label: Option[Varchar[W.`100`.T]],
  icon: Option[Varchar[W.`255`.T]],
  sortOrder: Int,
  isActive: Boolean,
  createdAt: ZonedDateTime
)

object SocialLink {

  object sql {
    def insert(socialLink: SocialLink): ConnectionIO[Unit] = {
      val sql =
        """
          |INSERT INTO social_links (
          | id,
          | platform,
          | url,
          | label,
          | icon,
          | sort_order,
          | is_active,
          | created_at
          |) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
          |""".stripMargin
      Update[SocialLink](sql).run(socialLink).void
    }
  }
}
