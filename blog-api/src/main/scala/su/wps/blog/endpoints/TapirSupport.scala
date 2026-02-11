package su.wps.blog.endpoints

import io.circe.{Decoder, Encoder}
import sttp.tapir.{Schema, SchemaType, Validator}
import su.wps.blog.models.api.*
import su.wps.blog.models.domain.{CommentId, PostId, TagId}

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object TapirSupport {

  implicit val postIdSchema: Schema[PostId] =
    Schema.schemaForInt.map(i => Some(PostId(i)))(_.value)
  implicit val tagIdSchema: Schema[TagId] =
    Schema.schemaForInt.map(i => Some(TagId(i)))(_.value)
  implicit val commentIdSchema: Schema[CommentId] =
    Schema.schemaForInt.map(i => Some(CommentId(i)))(_.value)

  implicit val zonedDateTimeSchema: Schema[ZonedDateTime] =
    Schema.string.format("date-time")

  implicit val postIdDecoder: Decoder[PostId] = Decoder[Int].map(PostId(_))
  implicit val tagIdDecoder: Decoder[TagId] = Decoder[Int].map(TagId(_))
  implicit val commentIdDecoder: Decoder[CommentId] = Decoder[Int].map(CommentId(_))

  implicit val zonedDateTimeDecoder: Decoder[ZonedDateTime] =
    Decoder[String].emap { s =>
      try Right(ZonedDateTime.parse(s, DateTimeFormatter.ISO_DATE_TIME))
      catch { case e: Exception => Left(e.getMessage) }
    }

  implicit val tagResultDecoder: Decoder[TagResult] =
    Decoder.forProduct3("id", "name", "slug")(TagResult.apply)

  implicit val postResultDecoder: Decoder[PostResult] =
    Decoder.forProduct4("name", "text", "created_at", "tags")(PostResult.apply)

  implicit val listPostResultDecoder: Decoder[ListPostResult] =
    Decoder.forProduct5("id", "name", "short_text", "created_at", "tags")(
      ListPostResult.apply
    )

  implicit def listItemsResultDecoder[T: Decoder]: Decoder[ListItemsResult[T]] =
    Decoder.forProduct2("items", "total")(ListItemsResult.apply[T])

  implicit lazy val commentResultDecoder: Decoder[CommentResult] = Decoder.instance { c =>
    for {
      id <- c.downField("id").as[CommentId]
      name <- c.downField("name").as[String]
      text <- c.downField("text").as[String]
      rating <- c.downField("rating").as[Int]
      createdAt <- c.downField("created_at").as[ZonedDateTime]
      replies <- c.downField("replies").as[List[CommentResult]]
    } yield CommentResult(id, name, text, rating, createdAt, replies)
  }

  implicit val commentsListResultDecoder: Decoder[CommentsListResult] =
    Decoder.forProduct2("comments", "total")(CommentsListResult.apply)

  implicit val tagWithCountResultDecoder: Decoder[TagWithCountResult] =
    Decoder.forProduct4("id", "name", "slug", "post_count")(TagWithCountResult.apply)

  implicit val tagCloudItemDecoder: Decoder[TagCloudItem] =
    Decoder.forProduct4("name", "slug", "count", "weight")(TagCloudItem.apply)

  implicit val tagCloudResultDecoder: Decoder[TagCloudResult] =
    Decoder.forProduct1("tags")(TagCloudResult.apply)

  implicit val pageResultDecoder: Decoder[PageResult] =
    Decoder.forProduct5("id", "url", "title", "content", "created_at")(PageResult.apply)

  implicit val listPageResultDecoder: Decoder[ListPageResult] =
    Decoder.forProduct2("url", "title")(ListPageResult.apply)

  implicit val healthResponseDecoder: Decoder[HealthResponse] =
    Decoder.forProduct3("status", "database", "timestamp")(HealthResponse.apply)

  implicit val errorResponseDecoder: Decoder[ErrorResponse] =
    Decoder.forProduct3("code", "message", "details")(ErrorResponse.apply)

  implicit val tagResultSchema: Schema[TagResult] =
    Schema.derived[TagResult]

  implicit val postResultSchema: Schema[PostResult] =
    Schema.derived[PostResult]

  implicit val listPostResultSchema: Schema[ListPostResult] =
    Schema.derived[ListPostResult]

  implicit def listItemsResultSchema[T: Schema]: Schema[ListItemsResult[T]] =
    Schema.derived[ListItemsResult[T]]

  implicit lazy val commentResultSchema: Schema[CommentResult] =
    Schema.derived[CommentResult]

  implicit val commentsListResultSchema: Schema[CommentsListResult] =
    Schema.derived[CommentsListResult]

  implicit val tagWithCountResultSchema: Schema[TagWithCountResult] =
    Schema.derived[TagWithCountResult]

  implicit val tagCloudItemSchema: Schema[TagCloudItem] =
    Schema.derived[TagCloudItem]

  implicit val tagCloudResultSchema: Schema[TagCloudResult] =
    Schema.derived[TagCloudResult]

  implicit val pageResultSchema: Schema[PageResult] =
    Schema.derived[PageResult]

  implicit val listPageResultSchema: Schema[ListPageResult] =
    Schema.derived[ListPageResult]

  implicit val healthResponseSchema: Schema[HealthResponse] =
    Schema.derived[HealthResponse]

  implicit val errorResponseSchema: Schema[ErrorResponse] =
    Schema.derived[ErrorResponse]

  implicit val createCommentRequestSchema: Schema[CreateCommentRequest] =
    Schema.derived[CreateCommentRequest]

  implicit val rateCommentRequestSchema: Schema[RateCommentRequest] =
    Schema.derived[RateCommentRequest]
}
