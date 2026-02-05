package su.wps.blog.validation

import cats.data.ValidatedNec
import cats.syntax.apply.*
import cats.syntax.validated.*

object Validation {
  val MinLimit = 1
  val MaxLimit = 100
  val MinOffset = 0
  val MaxCommentNameLength = 255
  val MaxCommentTextLength = 10000
  val MaxCommentEmailLength = 255

  private val EmailPattern = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$".r

  type FieldError = (String, String)

  def validateLimit(limit: Int): ValidatedNec[FieldError, Int] =
    if (limit >= MinLimit && limit <= MaxLimit) limit.validNec
    else ("limit" -> s"Must be between $MinLimit and $MaxLimit").invalidNec

  def validateOffset(offset: Int): ValidatedNec[FieldError, Int] =
    if (offset >= MinOffset) offset.validNec
    else ("offset" -> "Must be non-negative").invalidNec

  def validatePagination(
    limit: Int,
    offset: Int
  ): ValidatedNec[FieldError, (Int, Int)] =
    (validateLimit(limit), validateOffset(offset)).mapN((l, o) => (l, o))

  def validateCommentName(name: String): ValidatedNec[FieldError, String] =
    if (name.trim.isEmpty) ("name" -> "Must not be empty").invalidNec
    else if (name.length > MaxCommentNameLength)
      ("name" -> s"Must not exceed $MaxCommentNameLength characters").invalidNec
    else name.trim.validNec

  def validateCommentEmail(email: String): ValidatedNec[FieldError, String] =
    if (email.trim.isEmpty) ("email" -> "Must not be empty").invalidNec
    else if (email.length > MaxCommentEmailLength)
      ("email" -> s"Must not exceed $MaxCommentEmailLength characters").invalidNec
    else if (EmailPattern.findFirstIn(email.trim).isEmpty)
      ("email" -> "Invalid format").invalidNec
    else email.trim.validNec

  def validateCommentText(text: String): ValidatedNec[FieldError, String] =
    if (text.trim.isEmpty) ("text" -> "Must not be empty").invalidNec
    else if (text.length > MaxCommentTextLength)
      ("text" -> s"Must not exceed $MaxCommentTextLength characters").invalidNec
    else sanitizeHtml(text.trim).validNec

  def validateComment(
    name: String,
    email: String,
    text: String
  ): ValidatedNec[FieldError, (String, String, String)] =
    (
      validateCommentName(name),
      validateCommentEmail(email),
      validateCommentText(text)
    ).mapN((n, e, t) => (n, e, t))

  def sanitizeHtml(input: String): String =
    input
      .replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")
      .replace("\"", "&quot;")
      .replace("'", "&#x27;")
}
