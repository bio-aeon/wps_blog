package su.wps.blog.repositories

import su.wps.blog.models.domain.{Post, PostId}
import su.wps.blog.repositories.sql.{PostSql, PostSqlImpl}
import tofu.doobie.LiftConnectionIO

final class PostRepositoryImpl[DB[_]] private (sql: PostSql[DB]) extends PostRepository[DB] {
  def findAllWithLimitAndOffset(limit: Int, offset: Int): DB[List[Post]] =
    sql.findAllWithLimitAndOffset(limit, offset)

  def findCount: DB[Int] =
    sql.findCount

  def findById(id: PostId): DB[Option[Post]] =
    sql.findById(id)

  def findAllWithLimitAndOffsetIncludeHidden(limit: Int, offset: Int): DB[List[Post]] =
    sql.findAllWithLimitAndOffsetIncludeHidden(limit, offset)

  def findCountIncludeHidden: DB[Int] =
    sql.findCountIncludeHidden

  def findByTagSlug(tagSlug: String, limit: Int, offset: Int): DB[List[Post]] =
    sql.findByTagSlug(tagSlug, limit, offset)

  def findCountByTagSlug(tagSlug: String): DB[Int] =
    sql.findCountByTagSlug(tagSlug)

  def incrementViews(id: PostId): DB[Int] =
    sql.incrementViews(id)

  def searchPosts(query: String, limit: Int, offset: Int): DB[List[Post]] =
    sql.searchPosts(query, limit, offset)

  def searchPostsCount(query: String): DB[Int] =
    sql.searchPostsCount(query)
}

object PostRepositoryImpl {
  def create[DB[_]: LiftConnectionIO]: PostRepositoryImpl[DB] = {
    val postSql = PostSqlImpl.create[DB]
    new PostRepositoryImpl[DB](postSql)
  }
}
