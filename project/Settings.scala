import sbt.Keys._
import sbt._

object Settings {
  lazy val commonSettings = Seq(
    organization := "com.github.BambooTuna",
    publishTo := Some(Resolver.file("BFPackage",file("."))(Patterns(true, Resolver.mavenStyleBasePattern))),
    scalaVersion := "2.12.8",
    resolvers += "Maven Repo on github" at "https://BambooTuna.github.io/CryptoLib",
    resolvers += "Maven Repo on github" at "https://BambooTuna.github.io/WebSocketManager",
    libraryDependencies ++= Seq(
      Circe.core,
      Circe.generic,
      Circe.parser,
      Akka.http,
      Akka.stream,
      Akka.slf4j,
      Enumeratum.version,
      Logback.classic,
      LogstashLogbackEncoder.encoder,
      Config.core,
      Airframe.di,
      Monix.version,
      "com.github.BambooTuna" %% "cryptolib" % "2.1.6-SNAPSHOT",
      "com.github.BambooTuna" %% "websocketmanager" % "1.0.2-SNAPSHOT",
    )
  )

}
