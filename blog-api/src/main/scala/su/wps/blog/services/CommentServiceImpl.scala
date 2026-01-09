package su.wps.blog.services

import cats.Monad
import cats.syntax.functor.*
import mouse.anyf.*
import su.wps.blog.models.api.{CommentResult, CommentsListResult}
import su.wps.blog.models.domain.{Comment, CommentId, PostId}
import su.wps.blog.repositories.CommentRepository
import tofu.doobie.transactor.Txr

final class CommentServiceImpl[F[_]: Monad, DB[_]: Monad] private (
  commentRepo: CommentRepository[DB],
  xa: Txr[F, DB]
) extends CommentService[F] {

  def getCommentsForPost(postId: PostId): F[CommentsListResult] =
    commentRepo
      .findCommentsByPostId(postId)
      .thrushK(xa.trans)
      .map { comments =>
        val tree = buildCommentTree(comments)
        CommentsListResult(tree, comments.size)
      }

  private def buildCommentTree(comments: List[Comment]): List[CommentResult] = {
    val byParent: Map[Option[Int], List[Comment]] = comments.groupBy(_.parentId)

    def buildNode(comment: Comment): CommentResult = {
      val replies = byParent.getOrElse(comment.id.map(_.value), Nil).map(buildNode)
      CommentResult(
        id = comment.id.getOrElse(CommentId(0)),
        name = comment.name,
        text = comment.text,
        rating = comment.rating,
        createdAt = comment.createdAt,
        replies = replies.sortBy(_.createdAt)
      )
    }

    byParent
      .getOrElse(None, Nil)
      .map(buildNode)
      .sortBy(_.createdAt)
  }
}

object CommentServiceImpl {
  def create[F[_]: Monad, DB[_]: Monad](
    commentRepo: CommentRepository[DB],
    xa: Txr[F, DB]
  ): CommentServiceImpl[F, DB] =
    new CommentServiceImpl(commentRepo, xa)
}
