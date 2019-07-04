package com.github.BambooTuna.BFPackage

import akka.actor.{Actor, ActorSystem}
import akka.stream.ActorMaterializer
import com.github.BambooTuna.BFPackage.BFServerMockActor._
import com.github.BambooTuna.CryptoLib.restAPI.client.bitflyer.APIList.{SimpleOrderBody, SimpleOrderResponse}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.concurrent.duration._
import scala.util.Random

class BFServerMockActor extends Actor {

  implicit val system: ActorSystem                        = context.system
  implicit val materializer: ActorMaterializer            = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  val logger                                              = LoggerFactory.getLogger(getClass)

  def receive = {
    case NewOrderMock(r) =>
      val response =
        generateDelay
          .map(_ => Right(SimpleOrderResponse(generateOrderId)))

      sender() ! response
    case other => logger.info(other.toString)
  }

  def generateDelay = {
    Future {
      val delay = (10000 + Random.nextInt(40000)).microseconds
      Thread.sleep(delay.toMillis)
    }
  }

  def generateOrderId = {
    System.currentTimeMillis.toString
  }

}

object BFServerMockActor {

  sealed trait Command
  case class NewOrderMock(request: SimpleOrderBody) extends Command
  case class CancelOrderMock(orderId: String) extends Command
  case object CancelAllOrderMock extends Command

}
