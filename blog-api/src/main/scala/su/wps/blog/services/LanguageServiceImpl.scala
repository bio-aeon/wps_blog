package su.wps.blog.services

import cats.Monad
import cats.syntax.applicative.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import io.scalaland.chimney.dsl.*
import mouse.anyf.*
import su.wps.blog.models.api.LanguageResult
import su.wps.blog.repositories.LanguageRepository
import tofu.doobie.transactor.Txr

final class LanguageServiceImpl[F[_]: Monad, DB[_]: Monad] private (
  languageRepo: LanguageRepository[DB],
  xa: Txr[F, DB]
) extends LanguageService[F] {

  private val DefaultLang = "en"

  def getActiveLanguages: F[List[LanguageResult]] =
    languageRepo.findActive
      .thrushK(xa.trans)
      .map(_.map { lang =>
        LanguageResult(lang.code, lang.name, lang.nativeName, lang.isDefault)
      })

  def getDefaultLanguageCode: F[String] =
    languageRepo.findDefault
      .thrushK(xa.trans)
      .map(_.map(_.code).getOrElse(DefaultLang))

  def resolveLanguage(explicit: Option[String], acceptHeader: Option[String]): F[String] =
    languageRepo.findActive
      .thrushK(xa.trans)
      .map { activeLangs =>
        val activeCodes = activeLangs.map(_.code).toSet
        val defaultCode = activeLangs.find(_.isDefault).map(_.code).getOrElse(DefaultLang)

        explicit.filter(activeCodes.contains) match {
          case Some(lang) => lang
          case None =>
            acceptHeader.flatMap(parseAcceptLanguage(_, activeCodes)) match {
              case Some(lang) => lang
              case None => defaultCode
            }
        }
      }

  private def parseAcceptLanguage(header: String, activeCodes: Set[String]): Option[String] = {
    val entries = header
      .split(',')
      .map(_.trim)
      .flatMap { entry =>
        val parts = entry.split(';')
        val lang = parts(0).trim.toLowerCase
        val quality = parts
          .find(_.trim.startsWith("q="))
          .flatMap(q => scala.util.Try(q.trim.drop(2).toDouble).toOption)
          .getOrElse(1.0)
        Some((lang, quality))
      }
      .sortBy(-_._2)

    entries.iterator
      .map(_._1)
      .flatMap { lang =>
        if (activeCodes.contains(lang)) Some(lang)
        else {
          val prefix = lang.takeWhile(_ != '-')
          if (activeCodes.contains(prefix)) Some(prefix) else None
        }
      }
      .nextOption()
  }
}

object LanguageServiceImpl {
  def create[F[_]: Monad, DB[_]: Monad](
    languageRepo: LanguageRepository[DB],
    xa: Txr[F, DB]
  ): LanguageServiceImpl[F, DB] =
    new LanguageServiceImpl(languageRepo, xa)
}
