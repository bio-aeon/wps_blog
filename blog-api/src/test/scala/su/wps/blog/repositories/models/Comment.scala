package su.wps.blog.repositories.models

import cats.syntax.functor.*
import doobie.*
import su.wps.blog.instances.time.*
import su.wps.blog.tools.types.*

import java.time.ZonedDateTime

final case class Comment(
  id: PosInt,
  text: Varchar[W.`1000`.T],
  name: Varchar[W.`255`.T],
  email: Varchar[W.`75`.T],
  postId: PosInt,
  parentId: Option[PosInt],
  rating: Int,
  isApproved: Boolean,
  createdAt: ZonedDateTime
)

object Comment {

  object sql {
    def insert(comment: Comment): ConnectionIO[Unit] = {
      val sql =
        """
          |INSERT INTO comments (
          | id,
          | text,
          | name,
          | email,
          | post_id,
          | parent_id,
          | rating,
          | is_approved,
          | created_at
          |) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
          |""".stripMargin
      Update[Comment](sql).run(comment).void
    }
  }
}
