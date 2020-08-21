package io.github.rogern.shorturl

import java.net.URI

import cats.effect._
import cats.effect.IO
import dev.profunktor.redis4cats.Redis
import dev.profunktor.redis4cats.codecs.Codecs
import dev.profunktor.redis4cats.codecs.splits.SplitEpi
import dev.profunktor.redis4cats.data.RedisCodec
import dev.profunktor.redis4cats.effect.Log.Stdout._

import scala.util.Try

class UrlRepo(redisUri: String) {
  implicit val cs: ContextShift[IO] = IO.contextShift(scala.concurrent.ExecutionContext.global)

  private val CounterKey = "CounterKey"

  private val stringLongEpi: SplitEpi[String, Long] =
    SplitEpi(s => Try(s.toLong).getOrElse(-1), _.toString)

  private val longCodec: RedisCodec[String, Long] =
    Codecs.derive(RedisCodec.Utf8, stringLongEpi)

  private val counterResource = Redis[IO].simple(redisUri, longCodec)
  private val urlResource = Redis[IO].simple(redisUri, RedisCodec.Utf8)

  def save(shortUrl: ShortUrl, url: URI): IO[Unit] = {
    urlResource.use { cmd =>
      for {
        _ <- cmd.set(url.toString, shortUrl.path) // reversed index
        _ <- cmd.set(shortUrl.path, url.toString)
      } yield ()
    }
  }

  def incrementAndGet(): IO[Long] = {
    counterResource.use(_.incr(CounterKey))
  }

  def get(url: URI): IO[Option[ShortUrl]] = {
    for {
      maybe <- getByKey(url.toString)
    } yield maybe.map(ShortUrl)
  }

  def get(shortUrl: ShortUrl): IO[Option[URI]] = {
    for {
      maybe <- getByKey(shortUrl.path)
    } yield maybe.map(URI.create)
  }

  private def getByKey(key: String): IO[Option[String]] = {
    urlResource.use { cmd =>
      for {
        r <- cmd.get(key)
      } yield r
    }
  }
}
