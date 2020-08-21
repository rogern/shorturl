package io.github.rogern.shorturl

import org.scalatest.wordspec.AnyWordSpec

class UrlServiceSpec extends AnyWordSpec {

  "UserService" when {
    "generateShortUrl" should {
      "make ShortUrl from number" in {
        assert(UrlService.generateShortUrl(71586177L, 0) === ShortUrl("VZw0ea"))
      }
    }
  }
}
