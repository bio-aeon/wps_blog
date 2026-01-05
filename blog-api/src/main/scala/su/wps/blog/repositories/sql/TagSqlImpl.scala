package su.wps.blog.repositories.sql

import cats.syntax.applicative.*
import cats.syntax.list.*
import cats.tagless.syntax.functorK.*
import doobie.*
import doobie.implicits.*
import su.wps.blog.models.domain.{PostId, Tag, TagId}
import tofu.doobie.LiftConnectionIO

final class TagSqlImpl private extends TagSql[ConnectionIO] {
  private val tableName: Fragment = Fragment.const("tags")

  def findByPostId(postId: PostId): ConnectionIO[List[Tag]] =
    sql"""
      SELECT t.name, t.slug, t.id
      FROM tags t
      INNER JOIN posts_tags pt ON t.id = pt.tag_id
      WHERE pt.post_id = ${postId.value}
      ORDER BY t.name
    """.query[Tag].to[List]

  def findByPostIds(postIds: List[PostId]): ConnectionIO[List[(PostId, Tag)]] =
    if (postIds.isEmpty) {
      List.empty[(PostId, Tag)].pure[ConnectionIO]
    } else {
      val ids = postIds.map(_.value)
      val inClause = Fragments.in(fr"pt.post_id", ids.toNel.get)
      sql"""
        SELECT pt.post_id, t.name, t.slug, t.id
        FROM tags t
        INNER JOIN posts_tags pt ON t.id = pt.tag_id
        WHERE $inClause
        ORDER BY t.name
      """
        .query[(PostId, String, String, TagId)]
        .to[List]
        .map(_.map { case (pid, name, slug, id) => (pid, Tag(name, slug, Some(id))) })
    }

  def findAll: ConnectionIO[List[Tag]] =
    (fr"SELECT name, slug, id FROM" ++ tableName ++ fr"ORDER BY name")
      .query[Tag]
      .to[List]

  def findAllWithPostCounts: ConnectionIO[List[(Tag, Int)]] =
    sql"""
      SELECT t.name, t.slug, t.id, COUNT(p.id) as post_count
      FROM tags t
      LEFT JOIN posts_tags pt ON t.id = pt.tag_id
      LEFT JOIN posts p ON pt.post_id = p.id AND p.is_hidden = false
      GROUP BY t.id, t.name, t.slug
      ORDER BY t.name
    """.query[(Tag, Int)].to[List]

  def findById(id: TagId): ConnectionIO[Option[Tag]] =
    (fr"SELECT name, slug, id FROM" ++ tableName ++ fr"WHERE id = ${id.value}")
      .query[Tag]
      .option
}

object TagSqlImpl {
  def create[DB[_]](implicit L: LiftConnectionIO[DB]): TagSql[DB] =
    new TagSqlImpl().mapK(L.liftF)
}
