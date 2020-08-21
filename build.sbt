val finchVersion = "0.32.1"
val circeVersion = "0.13.0"
val scalatestVersion = "3.2.1"
val redisVersion = "1.5.2"
val redis4catsVersion = "0.10.2"

lazy val root = (project in file("."))
  .settings(
    resolvers += "jitpack" at "https://jitpack.io",
    organization := "io.github.rogern",
    name := "shorturl",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.13.3",
    libraryDependencies ++= Seq(
      "com.github.finagle" %% "finchx-core" % finchVersion,
      "com.github.finagle" %% "finchx-circe" % finchVersion,
      "dev.profunktor" %% "redis4cats-effects" % redis4catsVersion,
      "dev.profunktor" %% "redis4cats-log4cats" % redis4catsVersion,
      "com.github.tarossi.embedded-redis" % "embedded-redis" % redisVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "org.scalatest" %% "scalatest" % scalatestVersion % "test"
    )
  )