package su.wps.blog.repositories.models

import cats.syntax.functor.*
import doobie.*
import su.wps.blog.tools.types.*

final case class CommentRater(id: PosInt, ip: Varchar[W.`39`.T], commentId: PosInt)

object CommentRater {

  object sql {
    def insert(rater: CommentRater): ConnectionIO[Unit] = {
      val sql =
        """
          |INSERT INTO comment_raters (
          | id,
          | ip,
          | comment_id
          |) VALUES (?, ?, ?)
          |""".stripMargin
      Update[CommentRater](sql).run(rater).void
    }
  }
}
