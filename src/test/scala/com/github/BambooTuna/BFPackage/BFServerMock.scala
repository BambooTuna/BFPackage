package com.github.BambooTuna.BFPackage

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.github.BambooTuna.CryptoLib.restAPI.client.bitflyer.APIList._
import com.github.BambooTuna.CryptoLib.restAPI.client.bitflyer.APIList.BitflyerEnumDefinition.OrderStatus
import org.slf4j.LoggerFactory

import scala.concurrent.{ ExecutionContextExecutor, Future }
import scala.concurrent.duration._
import scala.util.Random

class BFServerMock(implicit system: ActorSystem) {

  implicit val materializer: ActorMaterializer            = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  val logger                                              = LoggerFactory.getLogger(getClass)

  val order = scala.collection.mutable.HashMap.empty[String, SimpleOrderBody]

  def newOrder(request: SimpleOrderBody) =
    generateDelay
      .map(_ => {
        val orderId = generateOrderId
        order += (orderId -> request)
        Right(SimpleOrderResponse(orderId))
      })

  def getOrders = {
    generateDelay
      .map(_ => {
        val response: List[GetMyOrdersResponse] = order
          .map(o => {
            val orderId   = o._1
            val orderData = o._2
            GetMyOrdersResponse(
              1L,
              orderId,
              orderData.product_code,
              orderData.side,
              orderData.child_order_type,
              orderData.price,
              orderData.price,
              orderData.size,
              OrderStatus.ACTIVE,
              "",
              "",
              orderId,
              0,
              0,
              0,
              0
            )
          }).toList
        Right(response)
      })

  }

  private def generateDelay = {
    Future {
      val delay = (10000 + Random.nextInt(40000)).microseconds
      Thread.sleep(delay.toMillis)
    }
  }

  private def generateOrderId = {
    System.currentTimeMillis.toString
  }

}

object BFServerMock {

  sealed trait Command
  case class NewOrderMock(request: SimpleOrderBody) extends Command
  case class CancelOrderMock(orderId: String)       extends Command
  case object CancelAllOrderMock                    extends Command

}
