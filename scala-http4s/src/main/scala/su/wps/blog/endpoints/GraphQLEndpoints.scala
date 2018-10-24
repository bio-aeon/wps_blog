package su.wps.blog.endpoints

import cats.effect.Effect
import cats.implicits._
import io.circe.Json
import io.circe.optics.JsonPath._
import io.circe.parser._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.{HttpService, Response}
import sangria.ast.Document
import sangria.execution._
import sangria.marshalling.circe._
import sangria.parser.{QueryParser, SyntaxError}
import sangria.schema.{ObjectType, Schema, _}
import su.wps.blog.data.LiftFuture

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

object SchemaDefinition {
  val Query = ObjectType(
    "Query",
    fields[Unit, Unit](
      Field("test", StringType, resolve = ctx => "test resp")
    ))

  def createSchema() = Schema(Query)
}

class GraphQLEndpoints[F[_]: Effect: LiftFuture] extends Http4sDsl[F] {
  def endpoints: HttpService[F] = {
    HttpService[F] {
      case request @ POST -> Root / "graphql" =>
        request.as[Json].flatMap { body =>
          val query = root.query.string.getOption(body)
          val operationName = root.operationName.string.getOption(body)
          val variablesStr = root.variables.string.getOption(body)

          def execute = query.map(QueryParser.parse(_)) match {
            case Some(Success(ast)) =>
              variablesStr.map(parse) match {
                case Some(Left(error)) =>
                  BadRequest(formatError(error))
                case Some(Right(json)) =>
                  executeGraphQL(ast, operationName, json)
                case None =>
                  executeGraphQL(
                    ast,
                    operationName,
                    root.variables.json.getOption(body) getOrElse Json.obj())
              }
            case Some(Failure(error)) => BadRequest(formatError(error))
            case None                 => BadRequest(formatStringError("No query to execute"))
          }

          execute
        }
    }
  }

  private def executeGraphQL(query: Document,
                             operationName: Option[String],
                             variables: Json): F[Response[F]] = {
    implicitly[LiftFuture[F]].apply {
      Executor
        .execute(
          SchemaDefinition.createSchema(),
          query,
          (),
          variables = if (variables.isNull) Json.obj() else variables,
          operationName = operationName,
          exceptionHandler = exceptionHandler
        )
        .map(Ok(_))
        .recover {
          case error: QueryAnalysisError => BadRequest(error.resolveError)
          case error: ErrorWithResolver =>
            InternalServerError(error.resolveError)
        }
    }.flatten
  }

  private def formatError(error: Throwable): Json = error match {
    case syntaxError: SyntaxError =>
      Json.obj(
        "errors" -> Json.arr(Json.obj(
          "message" -> Json.fromString(syntaxError.getMessage),
          "locations" -> Json.arr(Json.obj(
            "line" -> Json.fromBigInt(syntaxError.originalError.position.line),
            "column" -> Json.fromBigInt(
              syntaxError.originalError.position.column)))
        )))
    case NonFatal(e) =>
      formatStringError(e.getMessage)
    case e => throw e
  }

  private def formatStringError(message: String): Json =
    Json.obj(
      "errors" -> Json.arr(Json.obj("message" -> Json.fromString(message))))

  private val exceptionHandler = ExceptionHandler {
    case (_, e) => HandledException(e.getMessage)
  }
}
