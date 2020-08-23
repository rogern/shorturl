package io.github.rogern.shorturl

import java.net.URI

import com.twitter.finagle.{Status, http}
import org.scalatest.flatspec._
import io.circe.generic.auto._
import io.finch._
import io.finch.circe._
import io.github.rogern.shorturl.Server.Payload
import org.scalatest.{BeforeAndAfter, OneInstancePerTest}
import redis.embedded.RedisServer

class ServerTest extends AnyFlatSpec with OneInstancePerTest with BeforeAndAfter {

  val redis = new RedisServer.Builder()
    .port(6379)
    .setting("bind 127.0.0.1")
    .build()

  val urlRepo = new UrlRepo("redis://localhost:6379")
  val api = new Server.Api(new UrlService(urlRepo))

  before { redis.start() }
  after { redis.stop() }

  "healthcheck" should "return Ok(200)" in {
    val Some(result) = api.healthcheck(Input.get("/")).awaitOutputUnsafe()
    assert(result.status === http.Status(200))
  }

  "create" should "return Created(201)" in {
    val payload = Payload("http://example.com")
    val Some(result) = api.create(Input.post("/create").withBody[Application.Json](payload)).awaitOutputUnsafe()
    assert(result.status === http.Status(201))
    assert(result.value.length > 0)
  }

  "create" should "return Ok(200) if already exists" in {
    urlRepo.save(ShortUrl("aaaccc"), URI.create("http://example.com")).unsafeRunSync()
    val payload = Payload("http://example.com")
    val Some(result) = api.create(Input.post("/create").withBody[Application.Json](payload)).awaitOutputUnsafe()
    assert(result.status === http.Status(200))
    assert(result.value === "aaaccc")
  }

  "get" should "return SeeOther(303) redirect to real url" in {
    urlRepo.save(ShortUrl("abcdef"), URI.create("http://example.com")).unsafeRunSync()
    val Some(result) = api.lookup(Input.get("/abcdef")).awaitOutputUnsafe()
    assert(result.status === http.Status(303))
    assert(result.headers.get("Location") === Some("http://example.com"))
  }
}