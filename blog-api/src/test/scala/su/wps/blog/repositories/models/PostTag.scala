package su.wps.blog.repositories.models

import cats.syntax.functor.*
import doobie.*
import su.wps.blog.instances.time.*
import su.wps.blog.tools.types.*

import java.time.ZonedDateTime

final case class PostTag(postId: PosInt, tagId: PosInt, createdAt: ZonedDateTime)

object PostTag {

  object sql {
    def insert(postTag: PostTag): ConnectionIO[Unit] = {
      val sql =
        """
          |INSERT INTO posts_tags (
          | post_id,
          | tag_id,
          | created_at
          |) VALUES (?, ?, ?)
          |""".stripMargin
      Update[PostTag](sql).run(postTag).void
    }
  }
}
