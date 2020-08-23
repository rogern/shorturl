# Short URL
A URL shortener REST API written in Scala 2.13 using Finch (https://finagle.github.io/finch/) and Redis (https://redis.io/)

### Setup
Scala 2.13 and SBT 1.3 is required.
For simplicity Redis is Embedded but can easily be replaced to a remote instance.

### Instructions
To run: `sbt "runMain io.github.rogern.shorturl.Server"`
Service starts on port `8081`

### Api
- Create new short URL:
`POST /create` with body `{"url": "<your-url>"}`
If not exists response code shall be `201` and the short url path is the body as well as in the `Location header`.
If exists, the repsonse code is 200 and body is the short url path.

- Lookup short URL:
`GET /<short-url>` shall if exists redirect to the stored real URL. Otherwise a 404 NotFound is returned.
  