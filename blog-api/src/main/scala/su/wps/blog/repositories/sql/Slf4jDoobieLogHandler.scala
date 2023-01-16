package su.wps.blog.repositories.sql

import cats.effect.{Sync, SyncIO}
import cats.syntax.functor._
import doobie.LogHandler
import doobie.util.log.{ExecFailure, LogEvent, ProcessingFailure, Success}
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object Slf4jDoobieLogHandler {

  def create[F[_]](implicit F: Sync[F]): F[LogHandler] =
    for {
      logger <- F.delay(Slf4jLogger.getLogger[SyncIO])
    } yield LogHandler(logDoobieEvent(logger)(_).unsafeRunSync())

  private def logDoobieEvent[F[_]](logger: Logger[F]): LogEvent => F[Unit] = {
    case Success(s, a, e1, e2) =>
      logger.trace {
        s"""Successful Statement Execution:
           |
           |  ${s.linesIterator.dropWhile(_.trim.isEmpty).mkString("\n  ")}
           |
           | arguments = [${a.mkString(", ")}]
           |   elapsed = ${e1.toMillis} ms exec + ${e2.toMillis} ms processing (${(e1 + e2).toMillis} ms total)
         """.stripMargin
      }

    case ProcessingFailure(s, a, e1, e2, t) =>
      logger.error {
        s"""Failed Resultset Processing:
           |
           |  ${s.linesIterator.dropWhile(_.trim.isEmpty).mkString("\n  ")}
           |
           | arguments = [${a.mkString(", ")}]
           |   elapsed = ${e1.toMillis} ms exec + ${e2.toMillis} ms processing (failed) (${(e1 + e2).toMillis} ms total)
           |   failure = ${t.getMessage}
         """.stripMargin
      }

    case ExecFailure(s, a, e1, t) =>
      logger.error {
        s"""Failed Statement Execution:
           |
           |  ${s.linesIterator.dropWhile(_.trim.isEmpty).mkString("\n  ")}
           |
           | arguments = [${a.mkString(", ")}]
           |   elapsed = ${e1.toMillis} ms exec (failed)
           |   failure = ${t.getMessage}
         """.stripMargin
      }
  }
}
