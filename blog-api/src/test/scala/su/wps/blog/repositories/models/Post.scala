package su.wps.blog.repositories.models

import cats.syntax.functor.*
import doobie.*
import doobie.postgres.implicits.*
import su.wps.blog.tools.types.*

import java.time.ZonedDateTime

final case class Post(
  id: PosInt,
  name: Varchar[W.`255`.T],
  shortText: Varchar[W.`1000`.T],
  text: Varchar[W.`1000`.T],
  authorId: PosInt,
  views: PosInt,
  metaTitle: Varchar[W.`255`.T],
  metaKeywords: Varchar[W.`255`.T],
  metaDescription: Varchar[W.`255`.T],
  isHidden: Boolean,
  createdAt: ZonedDateTime
)

object Post {

  object sql {
    def insert(post: Post): ConnectionIO[Unit] = {
      val sql =
        """
          |INSERT INTO posts (
          | id,
          | name,
          | short_text,
          | text,
          | author_id,
          | views,
          | meta_title,
          | meta_keywords,
          | meta_description,
          | is_hidden,
          | created_at
          |) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
          |""".stripMargin
      Update[Post](sql).run(post).void
    }
  }
}
