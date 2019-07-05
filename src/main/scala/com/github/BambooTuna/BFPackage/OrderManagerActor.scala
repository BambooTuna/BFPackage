package com.github.BambooTuna.BFPackage

import akka.actor.SupervisorStrategy.Restart
import akka.actor.{ Actor, ActorSystem, OneForOneStrategy, Props }
import akka.http.scaladsl.model.StatusCodes
import akka.stream.ActorMaterializer
import com.github.BambooTuna.BFPackage.OrderManagerActor.Options
import com.github.BambooTuna.BFPackage.RealTimePositionManager._
import com.github.BambooTuna.CryptoLib.restAPI.client.bitflyer.APIList._
import com.github.BambooTuna.CryptoLib.restAPI.client.bitflyer.BitflyerRestAPIs
import com.github.BambooTuna.CryptoLib.restAPI.model.Entity
import org.slf4j.LoggerFactory

import scala.concurrent.{ ExecutionContextExecutor, Future }
import scala.concurrent.duration._
import io.circe.generic.auto._

import scala.util.{ Failure, Success }

class OrderManagerActor(options: Options) extends Actor {

  implicit val system: ActorSystem                        = context.system
  implicit val materializer: ActorMaterializer            = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  val logger                                              = LoggerFactory.getLogger(getClass)
  val api                                                 = options.api

  val realTimePositionManager =
    context.actorOf(Props(classOf[RealTimePositionManager], RealTimePositionManager.Options(options.api)),
                    "RealTimePositionManager")

  def receive = {
    case orderData: SimpleOrderBody =>
      api.simpleOrder
        .run(
          entity = Some(
            Entity(orderData)
          )
        ).onComplete {
          case Success(orderResponse) =>
            if (orderResponse.isRight) orderSuccess(orderData, orderResponse.right.get)
            else logger.error(s"EntryOrder: Left: ${orderResponse.left.get}")
          case Failure(exception) => logger.error(s"EntryOrder: Error: ${exception.getMessage}")
        }
    case cancelOrder: CancelOrderBody =>
      api.cancelOrder
        .run(
          entity = Some(
            Entity(cancelOrder)
          )
        ).onComplete {
          case Success(response) =>
            if (response.isRight) cancelSuccess(cancelOrder.child_order_acceptance_id)
            else logger.error(s"CancelOrder: Left: ${response.left.get}")
          case Failure(exception) => logger.error(s"CancelOrder: Error: ${exception.getMessage}")
        }
    case cancelAllOrder: CancelAllOrderBody =>
      api.cancelAllOrder
        .run(
          entity = Some(
            Entity(cancelAllOrder)
          )
        ).onComplete {
          case Success(response) =>
            if (response.left.filter(_.statusCode == StatusCodes.OK).isDefined) cancelAllSuccess
            else logger.error(s"CancelAllOrder: UnknownError: $response")
          case Failure(exception) => logger.error(s"CancelAllOrder: Error: ${exception.getMessage}")
        }
    case v: RemindStatus => context.parent ! v
    case FetchStatus     => realTimePositionManager ! FetchStatus
    case other           => logger.debug(other.toString)
  }

  override def supervisorStrategy = OneForOneStrategy() {
    case _ =>
      Thread.sleep(5.seconds.toMillis)
      Restart
  }

  def orderSuccess(request: SimpleOrderBody, response: SimpleOrderResponse): Future[Unit] = {
    logger.debug(s"OrderSuccess: $request, response: $response")
    val orderId = response.child_order_acceptance_id
    realTimePositionManager ! AddOrderId(orderId, request)
    val delay =
      if (request.time_in_force == "GTC") request.minute_to_expire.minutes
      else request.minute_to_expire.minutes
    setCancelOrderTimer(orderId, delay)
  }

  def cancelSuccess(orderId: String) = {
    realTimePositionManager ! CanceledOrderId(orderId)
  }

  def cancelAllSuccess = {
    realTimePositionManager ! CanceledAllOrderId
  }

  def setCancelOrderTimer(orderId: String, delay: FiniteDuration): Future[Unit] = {
    Future {
      Thread.sleep(delay.toMillis)
      realTimePositionManager ! CanceledOrderId(orderId)
    }
  }
}

object OrderManagerActor {

  case class Options(
      api: BitflyerRestAPIs,
      debug: Boolean = false
  )

  sealed trait Command

}
