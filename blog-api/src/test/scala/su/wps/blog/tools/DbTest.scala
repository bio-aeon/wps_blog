package su.wps.blog.tools

import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Resource}
import com.dimafeng.testcontainers.PostgreSQLContainer
import doobie.Transactor
import fly4s.core._
import fly4s.core.data._
import org.specs2.specification.{BeforeAfterAll, BeforeAfterEach}
import org.testcontainers.utility.DockerImageName

trait DbTest extends BeforeAfterEach with BeforeAfterAll {

  private lazy val container: PostgreSQLContainer = PostgreSQLContainer(
    DockerImageName.parse("postgres:15.3")
  )

  implicit lazy val xa: Transactor[IO] = {
    Transactor.fromDriverManager[IO](
      container.driverClassName,
      container.jdbcUrl,
      container.username,
      container.password
    )
  }

  private lazy val fly4s: Resource[IO, Fly4s[IO]] = Fly4s
    .make[IO](
      container.jdbcUrl,
      Some(container.username),
      Some(container.password.toCharArray),
      Fly4sConfig(defaultSchemaName = Some(container.databaseName), cleanDisabled = false)
    )

  def beforeAll(): Unit =
    container.start()

  def afterAll(): Unit =
    container.stop()

  override def before: Any =
    fly4s
      .use(_.migrate)
      .unsafeRunSync()

  override protected def after: Any =
    fly4s.use(_.clean).unsafeRunSync()
}
