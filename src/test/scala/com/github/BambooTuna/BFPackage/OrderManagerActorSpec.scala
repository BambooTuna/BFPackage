package com.github.BambooTuna.BFPackage

import akka.actor.{ Actor, ActorSystem, Props }
import akka.stream.ActorMaterializer
import com.github.BambooTuna.BFPackage.RealTimePositionManager.RemindStatus
import com.github.BambooTuna.CryptoLib.restAPI.client.bitflyer.APIList.BitflyerEnumDefinition.{ OrderType, Side }
import com.github.BambooTuna.CryptoLib.restAPI.client.bitflyer.APIList.{ CancelAllOrderBody, SimpleOrderBody }
import com.github.BambooTuna.CryptoLib.restAPI.client.bitflyer.BitflyerRestAPIs
import com.github.BambooTuna.CryptoLib.restAPI.model.ApiKey
import org.slf4j.LoggerFactory

import scala.concurrent.{ ExecutionContextExecutor, Future }

object OrderManagerActorRootSpec extends App {

  implicit val system: ActorSystem                        = ActorSystem("OrderManagerActorRootSpec")
  implicit val materializer: ActorMaterializer            = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  val logger                                              = LoggerFactory.getLogger(getClass)

  val bitflyerRestAPIs = new BitflyerRestAPIs(ApiKey("key", "secret"))

  system.actorOf(Props(classOf[OrderManagerActorSpec], bitflyerRestAPIs), "OrderManagerActorSpec")

}

class OrderManagerActorSpec(api: BitflyerRestAPIs) extends Actor {

  implicit val system: ActorSystem                        = context.system
  implicit val materializer: ActorMaterializer            = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  val logger                                              = LoggerFactory.getLogger(getClass)

  val orderManagerActorSpec = context.actorOf(Props(classOf[OrderManagerActor],
                                                    OrderManagerActor.Options(
                                                      api = api
                                                    )),
                                              "OrderManagerActorSpec")

  def receive = {
    case RemindStatus(order, size) =>
      logger.info(s"Order: $order, Size: $size")
    case other =>
      logger.info(other.toString)
  }

  val orderData = SimpleOrderBody(
    product_code = "FX_BTC_JPY",
    child_order_type = OrderType.Market,
    side = Side.Sell,
    price = 0L,
    size = 0.01,
    minute_to_expire = 1L
  )
  orderManagerActorSpec ! CancelAllOrderBody()

//  Future {
//    Thread.sleep(5000)
//    orderManagerActorSpec ! FetchStatus
//  }

}
