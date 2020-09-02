package io.github.rogern.shorturl

import java.net.URI

import cats.effect.IO
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.finagle.{Http, Service}
import com.twitter.util.Await
import io.circe.generic.auto._
import io.finch._
import io.finch.catsEffect._
import io.finch.circe._
import redis.embedded.RedisServer

import scala.util.Try

object Server extends App {

  case class Payload(url: String)

  class Api(urlService: UrlService) {

    def healthcheck: Endpoint[IO, String] = get(pathEmpty) {
      Ok("OK")
    }

    def create: Endpoint[IO, String] = post("create" :: jsonBody[Payload]) { p: Payload =>
      for {
        valid <- IO.fromTry(Try(URI.create(p.url)))
        result <- urlService.createOrGet(valid)
      } yield {
        result match {
          case Result.Existing(ShortUrl(url)) => Ok(url)
          case Result.Created(ShortUrl(url)) => Created(url).withHeader("Location" -> url)
        }
      }
    }

    def lookup: Endpoint[IO, Unit] = get(path[String]) { path: String =>
      urlService.get(ShortUrl(path)) map {
        case Left(Missing) => Output.empty(Status.NotFound)
        case Right(url) => Output.unit(Status.SeeOther).withHeader("Location" -> url.toASCIIString)
      }
    }

    def run = healthcheck :+: create :+: lookup
  }

  private val redis = new RedisServer.Builder()
    .port(6379)
    .setting("bind 127.0.0.1")
    .build()

  redis.start()

  private val urlService = new UrlService(new UrlRepo("redis://localhost:6379"))

  def service: Service[Request, Response] = Bootstrap
    .serve[Text.Plain](new Api(urlService).run)
    .toService

  Runtime.getRuntime.addShutdownHook(new Thread(() => redis.stop()))
  Await.ready(Http.server.serve(":8081", service))
}