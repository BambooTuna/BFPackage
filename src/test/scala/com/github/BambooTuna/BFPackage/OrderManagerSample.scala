package com.github.BambooTuna.BFPackage

import akka.actor.{Actor, ActorSystem, Props}
import akka.stream.ActorMaterializer
import com.github.BambooTuna.BFPackage.RealTimePositionManager.{FetchStatus, RemindStatus}
import com.github.BambooTuna.CryptoLib.restAPI.client.bitflyer.APIList.BitflyerEnumDefinition.{OrderType, Side}
import com.github.BambooTuna.CryptoLib.restAPI.client.bitflyer.APIList.{CancelAllOrderBody, CancelOrderBody, SimpleOrderBody}
import com.github.BambooTuna.CryptoLib.restAPI.client.bitflyer.BitflyerRestAPIs
import com.github.BambooTuna.CryptoLib.restAPI.model.ApiKey
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContextExecutor

object OrderManagerSampleRoot extends App {

  implicit val system: ActorSystem                        = ActorSystem("OrderManagerSampleRoot")
  implicit val materializer: ActorMaterializer            = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  val logger                                              = LoggerFactory.getLogger(getClass)

  val bitflyerRestAPIs = new BitflyerRestAPIs(ApiKey("key", "secret"))

  system.actorOf(Props(classOf[OrderManagerSampleActor], bitflyerRestAPIs), "OrderManagerSampleActor")

}

class OrderManagerSampleActor(api: BitflyerRestAPIs) extends Actor {

  implicit val system: ActorSystem                        = context.system
  implicit val materializer: ActorMaterializer            = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  val logger                                              = LoggerFactory.getLogger(getClass)

  val orderManagerActorSpec =
    context.actorOf(Props(classOf[OrderManagerActor],
      RealTimePositionManager.Options(
        api = api
      )),
      "OrderManagerActorSpec")

  def receive = {
    case RemindStatus(order, size) =>
      //ポジション数量に変動があるとここが呼ばれて
      //オープン注文、ポジション数量が出力されます。
      logger.info(s"Order: $order, Size: $size")
    case other =>
      logger.debug(other.toString)
  }

  //任意のタイミングで情報を取得することもできます。
  //receive#RemindStatus(order, size)にデータが送られます。
  orderManagerActorSpec ! FetchStatus

  //新規注文
  orderManagerActorSpec ! SimpleOrderBody(
    product_code = "FX_BTC_JPY",
    child_order_type = OrderType.Limit,
    side = Side.Buy,
    price = 1000000L,
    size = 0.01,
    minute_to_expire = 1L,
    time_in_force = "GTC"
  )

  //キャンセル
  orderManagerActorSpec ! CancelOrderBody(
    product_code = "FX_BTC_JPY",
    child_order_acceptance_id = "orderId"
  )

  //全キャンセル
  orderManagerActorSpec ! CancelAllOrderBody()

}
