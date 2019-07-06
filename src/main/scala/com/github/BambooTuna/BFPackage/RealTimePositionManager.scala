package com.github.BambooTuna.BFPackage

import akka.actor.SupervisorStrategy.Restart
import akka.actor.{ Actor, ActorSystem, OneForOneStrategy, Props }
import akka.stream.ActorMaterializer
import com.github.BambooTuna.BFPackage.EnumDefinition.StreamChannel
import com.github.BambooTuna.BFPackage.RealTimePositionManager._
import com.github.BambooTuna.BFPackage.StreamActor.{ Execution, LightningExecutions, StreamDataError }
import com.github.BambooTuna.CryptoLib.restAPI.client.bitflyer.APIList.BitflyerEnumDefinition.Side
import com.github.BambooTuna.CryptoLib.restAPI.client.bitflyer.APIList.{
  GetMyOrdersQueryParameters,
  GetMyPositionsQueryParameters,
  SimpleOrderBody
}
import com.github.BambooTuna.CryptoLib.restAPI.client.bitflyer.BitflyerRestAPIs
import com.github.BambooTuna.CryptoLib.restAPI.model.QueryParameters
import org.slf4j.LoggerFactory

import scala.concurrent.{ Await, ExecutionContextExecutor, Future }
import scala.concurrent.duration._
import io.circe.generic.auto._

import scala.util.{ Failure, Success }

private[BFPackage] class RealTimePositionManager(options: Options) extends Actor {

  implicit val system: ActorSystem                        = context.system
  implicit val materializer: ActorMaterializer            = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  val logger                                              = LoggerFactory.getLogger(getClass)
  val api                                                 = options.api

  val order                    = scala.collection.mutable.HashMap.empty[String, OrderData]
  var positionSize: BigDecimal = 0
  val executed                 = scala.collection.mutable.ArrayBuffer.empty[ExecutedData]

  val webSocketManager =
    context.actorOf(Props(classOf[StreamActor], StreamChannel.Executions_FX, false), "Executions_FX")

  system.scheduler.schedule(1.hours, 1.hours, self, InitData)

  def receive = {
    case InitData =>
      if (init) remindStatusToParent
    case AddOrderId(id, simpleOrderBody) =>
      order += (id ->
      OrderData(
        simpleOrderBody.side,
        simpleOrderBody.price,
        simpleOrderBody.size
      ))
    case CanceledAllOrderId =>
      //使用しないことを推奨
      order.clear()
      remindStatusToParent
    case CanceledOrderId(id) =>
      Future {
        Thread.sleep(5.seconds.toMillis)
        if (order.contains(id)) order -= id
        remindStatusToParent
      }
    case ExecutedOrderId(id, executedData) =>
      if (order.contains(id)) {
        val orderData     = order(id)
        val remainingSize = orderData.size - executedData.size
        order -= id
        if (remainingSize > 0) order += (id -> orderData.copy(size = remainingSize))
        positionSize += executedData.size * convertSideToBigDecimal(executedData.side)
        //executed += executedData
        remindStatusToParent
      }
    case LightningExecutions(list) =>
      list.foreach { execution =>
        order
          .get(execution.buy_child_order_acceptance_id)
          .map(
            _ =>
              ExecutedOrderId(execution.buy_child_order_acceptance_id,
                              convertExecutionDataToExecutedData(Side.Buy, execution))
          )
          .foreach(self ! _)
        order
          .get(execution.sell_child_order_acceptance_id)
          .map(
            _ =>
              ExecutedOrderId(execution.sell_child_order_acceptance_id,
                              convertExecutionDataToExecutedData(Side.Sell, execution))
          )
          .foreach(self ! _)
      }
    case FetchStatus        => sender() ! RemindStatus(order.toMap, positionSize)
    case InternalError(e)   => throw e
    case _: StreamDataError => Unit
    case other              => logger.debug(other.toString)
  }

  override def preStart() = {
    super.preStart()
    self ! InitData
  }

  override def supervisorStrategy = OneForOneStrategy() {
    case _ =>
      Thread.sleep(5.seconds.toMillis)
      self ! InitData
      Restart
  }

  def remindStatusToParent =
    context.parent ! RemindStatus(order.toMap, positionSize)

  def convertExecutionDataToExecutedData(side: Side, execution: Execution): ExecutedData =
    ExecutedData(side, execution.price, execution.size)

  def convertSideToBigDecimal(side: Side): BigDecimal = side match {
    case Side.Buy  => 1
    case Side.Sell => -1
  }

  def init: Boolean = {
    val getPositions = api.getMyPositions.run(
      queryParameters = Some(
        QueryParameters(
          GetMyPositionsQueryParameters()
        )
      )
    )
    val getOrders = api.getMyOrders.run(
      queryParameters = Some(
        QueryParameters(
          GetMyOrdersQueryParameters()
        )
      )
    )
    val clearFuture = for {
      positionData <- getPositions
      orderData    <- getOrders
      if positionData.isRight
      if orderData.isRight
    } yield {
      val newSize = positionData.right.get.map(p => p.size * convertSideToBigDecimal(p.side)).sum
      logger.info(
        s"${if (positionSize == newSize) "No difference!" else "Difference..."} | positionSize: $positionSize, newSize: $newSize"
      )
      positionSize = newSize
      order.clear()
      executed.clear()
      order ++= orderData.right.get.map(o => (o.child_order_acceptance_id, OrderData(o.side, o.price, o.size))).toMap
      true
    }
    clearFuture.onComplete {
      case Success(value)     => value
      case Failure(exception) => self ! InternalError(InitDataException(s"${exception.getMessage}"))
    }
    try {
      Await.result(clearFuture, 5.seconds)
    } catch {
      case _: Exception => false
    }
  }

}

object RealTimePositionManager {

  case class Options(
      api: BitflyerRestAPIs,
      debug: Boolean = false
  )

  sealed trait Command
  case class AddOrderId(orderId: String, orderData: SimpleOrderBody)               extends Command
  case object CanceledAllOrderId                                                   extends Command
  case class CanceledOrderId(orderId: String)                                      extends Command
  private case class ExecutedOrderId(orderId: String, executedData: ExecutedData)  extends Command
  case class RemindStatus(order: Map[String, OrderData], positionSize: BigDecimal) extends Command
  case object FetchStatus                                                          extends Command

  case object InitData                           extends Command
  case class InternalError(exception: Exception) extends Command

  case class OrderData(side: Side, price: Long, size: BigDecimal) {
    require(size > 0)
  }
  case class ExecutedData(side: Side, price: Long, size: BigDecimal) {
    require(size > 0)
  }
  case class InitDataException(errorMessage: String) extends Exception(errorMessage)

}
