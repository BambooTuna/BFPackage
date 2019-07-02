package com.github.BambooTuna.BFPackage.Notification

import akka.actor.{Actor, ActorSystem}
import akka.stream.ActorMaterializer
import com.github.BambooTuna.BFPackage.Notification.DiscordPushActor.{DiscordPushSettings, SendNotification}
import com.github.BambooTuna.CryptoLib.restAPI.client.discord.APIList.WebhookBody
import com.github.BambooTuna.CryptoLib.restAPI.client.discord.DiscordRestAPIs
import com.github.BambooTuna.CryptoLib.restAPI.model.Entity
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContextExecutor
import io.circe.generic.auto._

class DiscordPushActor(discordPushSettings: DiscordPushSettings) extends Actor {

  implicit val system: ActorSystem = context.system
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  val logger = LoggerFactory.getLogger(getClass)

  val api = discordPushSettings.discordRestAPIs
  val botOptions = discordPushSettings.botOptions
  val debug = discordPushSettings.debug

  def receive = {
    case SendNotification(m) => sendMessage(m)
    case other => logger.debug(other.toString)
  }

  def sendMessage(message: String) = {
    api.webhook.run(
      entity = Some(
        Entity(
          WebhookBody(
            username = botOptions.name,
            content = message
          )
        )
      )
    )
  }

}

object DiscordPushActor {
  sealed trait Command
  case class SendNotification(message: String) extends Command

  case class BotOptions(name: String = "DiscordBot")
  case class DiscordPushSettings(
                                  discordRestAPIs: DiscordRestAPIs,
                                  botOptions: BotOptions = BotOptions(),
                                  debug: Boolean = false
                                )
}