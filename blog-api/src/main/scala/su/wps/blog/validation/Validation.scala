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

  val MaxContactNameLength = 255
  val MaxContactEmailLength = 255
  val MaxContactSubjectLength = 500
  val MaxContactMessageLength = 5000
  val MinContactSubjectLength = 3
  val MinContactMessageLength = 10

  def validateContactName(name: String): ValidatedNec[FieldError, String] =
    if (name.trim.isEmpty) ("name" -> "Must not be empty").invalidNec
    else if (name.length > MaxContactNameLength)
      ("name" -> s"Must not exceed $MaxContactNameLength characters").invalidNec
    else name.trim.validNec

  def validateContactEmail(email: String): ValidatedNec[FieldError, String] =
    if (email.trim.isEmpty) ("email" -> "Must not be empty").invalidNec
    else if (email.length > MaxContactEmailLength)
      ("email" -> s"Must not exceed $MaxContactEmailLength characters").invalidNec
    else if (EmailPattern.findFirstIn(email.trim).isEmpty)
      ("email" -> "Invalid format").invalidNec
    else email.trim.validNec

  def validateContactSubject(subject: String): ValidatedNec[FieldError, String] =
    if (subject.trim.isEmpty) ("subject" -> "Must not be empty").invalidNec
    else if (subject.trim.length < MinContactSubjectLength)
      ("subject" -> s"Must be at least $MinContactSubjectLength characters").invalidNec
    else if (subject.length > MaxContactSubjectLength)
      ("subject" -> s"Must not exceed $MaxContactSubjectLength characters").invalidNec
    else subject.trim.validNec

  def validateContactMessage(message: String): ValidatedNec[FieldError, String] =
    if (message.trim.isEmpty) ("message" -> "Must not be empty").invalidNec
    else if (message.trim.length < MinContactMessageLength)
      ("message" -> s"Must be at least $MinContactMessageLength characters").invalidNec
    else if (message.length > MaxContactMessageLength)
      ("message" -> s"Must not exceed $MaxContactMessageLength characters").invalidNec
    else sanitizeHtml(message.trim).validNec

  def validateContact(
    name: String,
    email: String,
    subject: String,
    message: String
  ): ValidatedNec[FieldError, (String, String, String, String)] =
    (
      validateContactName(name),
      validateContactEmail(email),
      validateContactSubject(subject),
      validateContactMessage(message)
    ).mapN((n, e, s, m) => (n, e, s, m))

  def sanitizeHtml(input: String): String =
    input
      .replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")
      .replace("\"", "&quot;")
      .replace("'", "&#x27;")
}
