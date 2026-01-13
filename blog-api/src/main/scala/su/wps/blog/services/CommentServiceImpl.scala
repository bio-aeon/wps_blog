package su.wps.blog.services

import cats.Monad
import cats.data.OptionT
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import mouse.anyf.*
import su.wps.blog.models.api.{CommentResult, CommentsListResult, CreateCommentRequest}
import su.wps.blog.models.domain.{AppErr, Comment, CommentId, PostId}
import su.wps.blog.repositories.CommentRepository
import tofu.Raise
import tofu.doobie.transactor.Txr

import java.time.ZonedDateTime

final class CommentServiceImpl[F[_], DB[_]] private (
  commentRepo: CommentRepository[DB],
  xa: Txr[F, DB]
)(implicit F: Monad[F], DB: Monad[DB], R: Raise[F, AppErr])
    extends CommentService[F] {

  def getCommentsForPost(postId: PostId): F[CommentsListResult] =
    commentRepo
      .findCommentsByPostId(postId)
      .thrushK(xa.trans)
      .map { comments =>
        val tree = buildCommentTree(comments)
        CommentsListResult(tree, comments.size)
      }

  def createComment(postId: PostId, request: CreateCommentRequest): F[CommentResult] = {
    val comment = Comment(
      text = request.text,
      name = request.name,
      email = request.email,
      postId = postId.value,
      rating = 0,
      createdAt = ZonedDateTime.now(),
      parentId = request.parentId,
      id = None
    )

    commentRepo
      .insert(comment)
      .thrushK(xa.trans)
      .map(toCommentResult)
  }

  def rateComment(commentId: CommentId, isUpvote: Boolean, ip: String): F[Unit] = {
    val delta = if (isUpvote) 1 else -1
    val query = for {
      alreadyRated <- commentRepo.hasRated(commentId, ip)
      _ <-
        if (alreadyRated) DB.unit
        else commentRepo.insertRater(commentId, ip) >> commentRepo.updateRating(commentId, delta)
    } yield ()
    query.thrushK(xa.trans)
  }

  def deleteComment(commentId: CommentId): F[Unit] = {
    val query = for {
      commentOpt <- commentRepo.findById(commentId)
      _ <- commentOpt.fold(DB.unit)(_ => commentRepo.delete(commentId).void)
    } yield commentOpt
    OptionT(query.thrushK(xa.trans)).foldF(R.raise(AppErr.CommentNotFound(commentId)))(_ => F.unit)
  }

  def approveComment(commentId: CommentId): F[Unit] = {
    val query = for {
      commentOpt <- commentRepo.findById(commentId)
      _ <- commentOpt.fold(DB.unit)(_ => commentRepo.approve(commentId).void)
    } yield commentOpt
    OptionT(query.thrushK(xa.trans)).foldF(R.raise(AppErr.CommentNotFound(commentId)))(_ => F.unit)
  }

  private def toCommentResult(comment: Comment): CommentResult =
    CommentResult(
      id = comment.id.getOrElse(CommentId(0)),
      name = comment.name,
      text = comment.text,
      rating = comment.rating,
      createdAt = comment.createdAt,
      replies = Nil
    )

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
  def create[F[_]: Monad: Raise[*[_], AppErr], DB[_]: Monad](
    commentRepo: CommentRepository[DB],
    xa: Txr[F, DB]
  ): CommentServiceImpl[F, DB] =
    new CommentServiceImpl(commentRepo, xa)
}
