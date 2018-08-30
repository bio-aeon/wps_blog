package su.wps

import cats.~>

import scala.concurrent.Future

package object blog {
  object data {
    type TaglessFuture[F[_]] = Future ~> F
  }
}
