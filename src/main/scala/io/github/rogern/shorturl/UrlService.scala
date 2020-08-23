package io.github.rogern.shorturl

import java.net.URI

import cats.effect.IO
import io.github.rogern.shorturl.UrlService.generateShortUrl

trait Result

object Result {
  case class Existing(shortUrl: ShortUrl) extends Result
  case class Created(shortUrl: ShortUrl) extends Result
}

case object Missing

case class ShortUrl(path: String)

class UrlService(repo: UrlRepo) {

  import Result._

  def createOrGet(url: URI): IO[Result] = {

    def save(shortUrl: ShortUrl): IO[Result] = {
      repo
        .save(shortUrl, url)
        .map(_ => Created(shortUrl))
    }

    repo.get(url) flatMap {
      case Some(existing) => IO.pure(Existing(existing))
      case None =>
        for {
          counter <- repo.incrementAndGet()
          shortUrl = generateShortUrl(counter)
          result <- save(shortUrl)
        } yield result
    }
  }

  def get(shortUrl: ShortUrl): IO[Either[Missing.type, URI]] = {
    repo.get(shortUrl).map(_.toRight(Missing))
  }
}

object UrlService {

  val characters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
  val floor = 100_000_000L

  def generateShortUrl(num: Long, fl: Long = floor): ShortUrl = {
    def pickBase62(n: Long): Seq[Char] = {
      if (n > 0) characters.charAt((n % 62).toInt) +: pickBase62(n / 62)
      else Seq(characters.charAt((n % 62).toInt))
    }

    ShortUrl(pickBase62(num + fl).mkString)
  }
}
