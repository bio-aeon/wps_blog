package su.wps.blog.repositories.sql

import cats.tagless.syntax.functorK.*
import doobie.*
import doobie.implicits.*
import su.wps.blog.instances.time.*
import su.wps.blog.models.domain.SocialLink
import tofu.doobie.LiftConnectionIO

final class SocialLinkSqlImpl private extends SocialLinkSql[ConnectionIO] {

  def findAllActive: ConnectionIO[List[SocialLink]] =
    sql"""SELECT platform, url, label, icon, sort_order,
                 is_active, created_at, id
          FROM social_links
          WHERE is_active = true
          ORDER BY sort_order, id"""
      .query[SocialLink]
      .to[List]
}

object SocialLinkSqlImpl {
  def create[DB[_]](implicit L: LiftConnectionIO[DB]): SocialLinkSql[DB] =
    new SocialLinkSqlImpl().mapK(L.liftF)
}
