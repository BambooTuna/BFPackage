package com.github.BambooTuna.BFPackage

import akka.actor.SupervisorStrategy.Restart
import akka.actor.{ Actor, ActorSystem, OneForOneStrategy, Props }
import akka.stream.ActorMaterializer
import com.github.BambooTuna.BFPackage.EnumDefinition.StreamChannel
import com.github.BambooTuna.BFPackage.EnumDefinition.StreamChannel._
import com.github.BambooTuna.BFPackage.StreamActor._
import com.github.BambooTuna.CryptoLib.restAPI.client.bitflyer.APIList.BitflyerEnumDefinition
import com.github.BambooTuna.WebSocketManager.{ WebSocketManager, WebSocketOptions }
import com.github.BambooTuna.WebSocketManager.WebSocketProtocol.{
  ConnectStart,
  ConnectedSucceeded,
  OnMessage,
  SendMessage
}
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._

class StreamActor(channelName: StreamChannel) extends Actor {

  implicit val system: ActorSystem                        = context.system
  implicit val materializer: ActorMaterializer            = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  val logger                                              = LoggerFactory.getLogger(getClass)

  val webSocketManager = context.actorOf(
    Props(classOf[WebSocketManager], WebSocketOptions(host = "wss://ws.lightstream.bitflyer.com/json-rpc")),
    WebSocketManager.ActorName
  )

  override def preStart() = {
    super.preStart()
    logger.debug(s"preStart channelName: $channelName")
    webSocketManager ! ConnectStart
  }

  def receive = {
    case ConnectedSucceeded(ws) =>
      logger.debug("ConnectedSucceeded")
      ws ! SendMessage(subscribeMessage)
    case OnMessage(m) =>
      logger.debug(m)
      val org = parser.parse(m)

      val errorMessage = for {
        json <- org
        r    <- json.hcursor.downField("error").as[StreamDataError]
      } yield r

      val result = for {
        json        <- org
        channelName <- json.hcursor.downField("params").downField("channel").as[StreamChannel]
        r <- channelName match {
          case Executions_FX | Executions_Spot =>
            json.hcursor.downField("params").downField("message").as[Seq[Execution]].right.map(LightningExecutions)
          case Ticker_FX | Ticker_Spot => json.hcursor.downField("params").downField("message").as[LightningTicker]
          case Board_FX | Board_Spot   => json.hcursor.downField("params").downField("message").as[LightningBoard]
          case Board_snapshot_FX | Board_snapshot_Spot =>
            json.hcursor.downField("params").downField("message").as[LightningBoardSnapshot]
        }
      } yield r

      if (result.isRight) context.parent ! result.right.get
      else if (errorMessage.isRight) context.parent ! errorMessage.right.get
      else self ! InternalException(JsonParseException(result.toString))
    case InternalException(e) =>
      logger.debug(e.getMessage)
      throw e
    case other =>
      logger.debug(other.toString)
  }

  private val subscribeMessage = {
    case class Channel(channel: String)
    case class SubscribeLightningExecutions(method: String, params: Channel)
    SubscribeLightningExecutions("subscribe", Channel(channelName.value)).asJson.noSpaces
  }

  override def supervisorStrategy = OneForOneStrategy() {
    case _ =>
      Thread.sleep(5.seconds.toMillis)
      Restart
  }

}

object StreamActor {

  sealed trait StreamData
  case class Execution(
      id: Long,
      side: BitflyerEnumDefinition.Side,
      price: Long,
      size: BigDecimal,
      exec_date: String,
      buy_child_order_acceptance_id: String,
      sell_child_order_acceptance_id: String
  )
  case class LightningExecutions(executions: Seq[Execution]) extends StreamData
  case class LightningTicker(
      product_code: String,
      timestamp: String,
      tick_id: Long,
      best_bid: Long,
      best_ask: Long,
      best_bid_size: BigDecimal,
      best_ask_size: BigDecimal,
      total_bid_depth: BigDecimal,
      total_ask_depth: BigDecimal,
      ltp: Long,
      volume: BigDecimal,
      volume_by_product: BigDecimal
  ) extends StreamData
  case class Board(price: Long, size: BigDecimal)
  case class LightningBoard(mid_price: Long, bids: Seq[Board], asks: Seq[Board])         extends StreamData
  case class LightningBoardSnapshot(mid_price: Long, bids: Seq[Board], asks: Seq[Board]) extends StreamData
  case class StreamDataError(code: Long, message: String, data: String)                  extends StreamData

  case class InternalException(e: Exception)
  case class JsonParseException(errorMessage: String) extends Exception(errorMessage)

}
