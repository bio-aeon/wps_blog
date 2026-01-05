package su.wps.blog.repositories

import su.wps.blog.models.domain.{PostId, Tag, TagId}
import su.wps.blog.repositories.sql.{TagSql, TagSqlImpl}
import tofu.doobie.LiftConnectionIO

final class TagRepositoryImpl[DB[_]] private (sql: TagSql[DB]) extends TagRepository[DB] {
  def findByPostId(postId: PostId): DB[List[Tag]] =
    sql.findByPostId(postId)

  def findByPostIds(postIds: List[PostId]): DB[List[(PostId, Tag)]] =
    sql.findByPostIds(postIds)

  def findAll: DB[List[Tag]] =
    sql.findAll

  def findAllWithPostCounts: DB[List[(Tag, Int)]] =
    sql.findAllWithPostCounts

  def findById(id: TagId): DB[Option[Tag]] =
    sql.findById(id)
}

object TagRepositoryImpl {
  def create[DB[_]: LiftConnectionIO]: TagRepositoryImpl[DB] = {
    val tagSql = TagSqlImpl.create[DB]
    new TagRepositoryImpl[DB](tagSql)
  }
}
