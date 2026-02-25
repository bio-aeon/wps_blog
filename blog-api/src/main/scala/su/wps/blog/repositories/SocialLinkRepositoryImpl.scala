package su.wps.blog.repositories

import su.wps.blog.models.domain.SocialLink
import su.wps.blog.repositories.sql.{SocialLinkSql, SocialLinkSqlImpl}
import tofu.doobie.LiftConnectionIO

final class SocialLinkRepositoryImpl[DB[_]] private (sql: SocialLinkSql[DB])
    extends SocialLinkRepository[DB] {
  def findAllActive: DB[List[SocialLink]] = sql.findAllActive
}

object SocialLinkRepositoryImpl {
  def create[DB[_]: LiftConnectionIO]: SocialLinkRepositoryImpl[DB] = {
    val socialLinkSql = SocialLinkSqlImpl.create[DB]
    new SocialLinkRepositoryImpl[DB](socialLinkSql)
  }
}
