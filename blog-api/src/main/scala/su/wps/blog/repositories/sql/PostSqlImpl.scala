package su.wps.blog.repositories.sql

import cats.tagless.syntax.functorK.*
import doobie.*
import doobie.implicits.*
import tofu.doobie.LiftConnectionIO
import su.wps.blog.instances.time.*
import su.wps.blog.models.domain.{Post, PostId}

final class PostSqlImpl private extends PostSql[ConnectionIO] {
  val tableName: Fragment = Fragment.const("posts")

  def findAllWithLimitAndOffset(limit: Int, offset: Int): ConnectionIO[List[Post]] =
    (fr"SELECT name, short_text, text, author_id, views, meta_title, " ++
      fr"meta_keywords, meta_description, is_hidden, created_at, id FROM" ++ tableName ++
      fr"WHERE is_hidden = false ORDER BY created_at DESC LIMIT $limit OFFSET $offset")
      .query[Post]
      .to[List]

  def findCount: ConnectionIO[Int] =
    (fr"SELECT COUNT(*) FROM" ++ tableName ++ fr"WHERE is_hidden = false").query[Int].unique

  def findAllWithLimitAndOffsetIncludeHidden(limit: Int, offset: Int): ConnectionIO[List[Post]] =
    (fr"SELECT name, short_text, text, author_id, views, meta_title, " ++
      fr"meta_keywords, meta_description, is_hidden, created_at, id FROM" ++ tableName ++
      fr"ORDER BY created_at DESC LIMIT $limit OFFSET $offset")
      .query[Post]
      .to[List]

  def findCountIncludeHidden: ConnectionIO[Int] =
    (fr"SELECT COUNT(*) FROM" ++ tableName).query[Int].unique

  def findById(id: PostId): ConnectionIO[Option[Post]] =
    (fr"SELECT name, short_text, text, author_id, views, meta_title, " ++
      fr"meta_keywords, meta_description, is_hidden, created_at, id FROM" ++ tableName ++
      fr"WHERE id = $id")
      .query[Post]
      .option

  def findByTagSlug(tagSlug: String, limit: Int, offset: Int): ConnectionIO[List[Post]] =
    sql"""
      SELECT p.name, p.short_text, p.text, p.author_id, p.views,
             p.meta_title, p.meta_keywords, p.meta_description,
             p.is_hidden, p.created_at, p.id
      FROM posts p
      INNER JOIN posts_tags pt ON p.id = pt.post_id
      INNER JOIN tags t ON pt.tag_id = t.id
      WHERE t.slug = $tagSlug AND p.is_hidden = false
      ORDER BY p.created_at DESC
      LIMIT $limit OFFSET $offset
    """.query[Post].to[List]

  def findCountByTagSlug(tagSlug: String): ConnectionIO[Int] =
    sql"""
      SELECT COUNT(*)
      FROM posts p
      INNER JOIN posts_tags pt ON p.id = pt.post_id
      INNER JOIN tags t ON pt.tag_id = t.id
      WHERE t.slug = $tagSlug AND p.is_hidden = false
    """.query[Int].unique
}

object PostSqlImpl {
  def create[DB[_]](implicit L: LiftConnectionIO[DB]): PostSql[DB] =
    new PostSqlImpl().mapK(L.liftF)
}
