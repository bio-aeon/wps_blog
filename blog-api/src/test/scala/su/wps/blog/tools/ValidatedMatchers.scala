package su.wps.blog.tools

import cats.data.Validated
import org.specs2.matcher.Matcher

trait ValidatedMatchers {

  protected def beValid[A](expected: A): Matcher[Validated[_, A]] =
    new Matcher[Validated[_, A]] {
      def apply[S <: Validated[_, A]](t: org.specs2.matcher.Expectable[S]) =
        t.value match {
          case Validated.Valid(v) =>
            result(
              v == expected,
              s"${t.description} is Valid($expected)",
              s"${t.description} is Valid($v) but expected Valid($expected)",
              t
            )
          case Validated.Invalid(e) =>
            result(
              false,
              "",
              s"${t.description} is Invalid($e) but expected Valid($expected)",
              t
            )
        }
    }

  protected def beInvalid: Matcher[Validated[_, _]] =
    new Matcher[Validated[_, _]] {
      def apply[S <: Validated[_, _]](t: org.specs2.matcher.Expectable[S]) =
        t.value match {
          case Validated.Invalid(_) =>
            result(true, s"${t.description} is Invalid", "", t)
          case Validated.Valid(v) =>
            result(
              false,
              "",
              s"${t.description} is Valid($v) but expected Invalid",
              t
            )
        }
    }
}
