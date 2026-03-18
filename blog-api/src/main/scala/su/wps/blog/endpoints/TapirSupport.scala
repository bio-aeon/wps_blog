package su.wps.blog.endpoints

import io.circe.{Decoder, Encoder}
import sttp.tapir.{Schema, SchemaType, Validator}
import su.wps.blog.models.api.*
import su.wps.blog.models.domain.*

import java.time.{LocalDate, ZonedDateTime}
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

  implicit val seoResultDecoder: Decoder[SeoResult] =
    Decoder.forProduct3("title", "description", "keywords")(SeoResult.apply)

  implicit val postResultDecoder: Decoder[PostResult] =
    Decoder.forProduct8(
      "id",
      "name",
      "text",
      "created_at",
      "language",
      "tags",
      "seo",
      "available_languages"
    )(PostResult.apply)

  implicit val listPostResultDecoder: Decoder[ListPostResult] =
    Decoder.forProduct7(
      "id",
      "name",
      "short_text",
      "created_at",
      "language",
      "tags",
      "available_languages"
    )(ListPostResult.apply)

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
    Decoder.forProduct8(
      "id",
      "url",
      "title",
      "content",
      "created_at",
      "language",
      "seo",
      "available_languages"
    )(PageResult.apply)

  implicit val listPageResultDecoder: Decoder[ListPageResult] =
    Decoder.forProduct3("url", "title", "language")(ListPageResult.apply)

  implicit val healthResponseDecoder: Decoder[HealthResponse] =
    Decoder.forProduct3("status", "database", "timestamp")(HealthResponse.apply)

  implicit val errorResponseDecoder: Decoder[ErrorResponse] =
    Decoder.forProduct3("code", "message", "details")(ErrorResponse.apply)

  implicit val tagResultSchema: Schema[TagResult] =
    Schema.derived[TagResult]

  implicit val seoResultSchema: Schema[SeoResult] = Schema.derived

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

  // Business card feature ID schemas
  implicit val skillIdSchema: Schema[SkillId] =
    Schema.schemaForInt.map(i => Some(SkillId(i)))(_.value)
  implicit val experienceIdSchema: Schema[ExperienceId] =
    Schema.schemaForInt.map(i => Some(ExperienceId(i)))(_.value)
  implicit val socialLinkIdSchema: Schema[SocialLinkId] =
    Schema.schemaForInt.map(i => Some(SocialLinkId(i)))(_.value)

  implicit val localDateSchema: Schema[LocalDate] =
    Schema.schemaForString.map(s => Some(LocalDate.parse(s)))(_.toString)

  // Business card feature result schemas
  implicit val skillResultSchema: Schema[SkillResult] = Schema.derived
  implicit val skillCategoryResultSchema: Schema[SkillCategoryResult] = Schema.derived
  implicit val experienceResultSchema: Schema[ExperienceResult] = Schema.derived
  implicit val socialLinkResultSchema: Schema[SocialLinkResult] = Schema.derived
  implicit val contactResponseSchema: Schema[ContactResponse] = Schema.derived
  implicit val profileResultSchema: Schema[ProfileResult] = Schema.derived
  implicit val aboutResultSchema: Schema[AboutResult] = Schema.derived
  implicit val createContactRequestSchema: Schema[CreateContactRequest] = Schema.derived

  // Business card feature decoders
  implicit val skillIdDecoder: Decoder[SkillId] = Decoder[Int].map(SkillId(_))
  implicit val experienceIdDecoder: Decoder[ExperienceId] = Decoder[Int].map(ExperienceId(_))
  implicit val socialLinkIdDecoder: Decoder[SocialLinkId] = Decoder[Int].map(SocialLinkId(_))

  implicit val localDateDecoder: Decoder[LocalDate] =
    Decoder[String].emap { s =>
      try Right(LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE))
      catch { case e: Exception => Left(e.getMessage) }
    }

  implicit val skillResultDecoder: Decoder[SkillResult] =
    Decoder.forProduct6("id", "name", "slug", "category", "proficiency", "icon")(SkillResult.apply)

  implicit val skillCategoryResultDecoder: Decoder[SkillCategoryResult] =
    Decoder.forProduct2("category", "skills")(SkillCategoryResult.apply)

  implicit val experienceResultDecoder: Decoder[ExperienceResult] =
    Decoder.forProduct8(
      "id",
      "company",
      "position",
      "description",
      "start_date",
      "end_date",
      "location",
      "company_url"
    )(ExperienceResult.apply)

  implicit val socialLinkResultDecoder: Decoder[SocialLinkResult] =
    Decoder.forProduct5("id", "platform", "url", "label", "icon")(SocialLinkResult.apply)

  implicit val contactResponseDecoder: Decoder[ContactResponse] =
    Decoder.forProduct1("message")(ContactResponse.apply)

  implicit val profileResultDecoder: Decoder[ProfileResult] =
    Decoder.forProduct5("name", "title", "photo_url", "resume_url", "bio")(ProfileResult.apply)

  implicit val aboutResultDecoder: Decoder[AboutResult] =
    Decoder.forProduct4("profile", "skills", "experiences", "social_links")(AboutResult.apply)

  // Feed schemas and decoders
  implicit val feedPostItemSchema: Schema[FeedPostItem] = Schema.derived
  implicit val feedPageItemSchema: Schema[FeedPageItem] = Schema.derived
  implicit val feedTagItemSchema: Schema[FeedTagItem] = Schema.derived
  implicit val feedResultSchema: Schema[FeedResult] = Schema.derived

  implicit val feedPostItemDecoder: Decoder[FeedPostItem] =
    Decoder.forProduct8(
      "id",
      "name",
      "short_text",
      "meta_description",
      "created_at",
      "language",
      "tags",
      "available_languages"
    )(FeedPostItem.apply)

  implicit val feedPageItemDecoder: Decoder[FeedPageItem] =
    Decoder.forProduct3("url", "title", "created_at")(FeedPageItem.apply)

  implicit val feedTagItemDecoder: Decoder[FeedTagItem] =
    Decoder.forProduct2("name", "slug")(FeedTagItem.apply)

  implicit val feedResultDecoder: Decoder[FeedResult] =
    Decoder.forProduct3("posts", "pages", "tags")(FeedResult.apply)

  implicit val languageResultDecoder: Decoder[LanguageResult] =
    Decoder.forProduct4("code", "name", "native_name", "is_default")(LanguageResult.apply)
  implicit val languageResultSchema: Schema[LanguageResult] = Schema.derived
}
