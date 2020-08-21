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

object Main extends App {

  val redis = new RedisServer.Builder()
    .port(6379)
    .setting("bind 127.0.0.1")
    .build()

  redis.start()

  private val urlService = new UrlService(new UrlRepo("redis://localhost:6379"))

  case class Payload(url: String)

  def healthcheck: Endpoint[IO, String] = get(pathEmpty) {
    Ok("OK")
  }

  def create: Endpoint[IO, String] = post("create" :: jsonBody[Payload]) { p: Payload =>
    //todo validation
    println(s"got ${p.url}")

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
      case Left(Result.NotFound) => Output.empty(Status.NotFound)
      case Right(url) => Output.unit(Status.SeeOther).withHeader("Location" -> url.toASCIIString)
    }
  }

  def service: Service[Request, Response] = Bootstrap
    .serve[Text.Plain](healthcheck :+: create :+: lookup)
    .toService

  Await.ready(Http.server.serve(":8081", service))
  redis.stop()
}