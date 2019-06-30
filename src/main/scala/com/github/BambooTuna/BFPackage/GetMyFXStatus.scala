package com.github.BambooTuna.BFPackage

import akka.actor.{ Actor, ActorSystem }
import akka.stream.ActorMaterializer
import com.github.BambooTuna.BFPackage.GetMyFXStatus._
import com.github.BambooTuna.CryptoLib.restAPI.client.bitflyer.APIList.{
  BitflyerEnumDefinition,
  GetMyPositionsQueryParameters
}
import com.github.BambooTuna.CryptoLib.restAPI.client.bitflyer.BitflyerRestAPIs
import com.github.BambooTuna.CryptoLib.restAPI.model.QueryParameters
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global

import scala.concurrent.duration._
import scala.util.{ Failure, Success }
import io.circe.generic.auto._
import org.slf4j.LoggerFactory

class GetMyFXStatus(api: BitflyerRestAPIs, options: GMFSOptions = GMFSOptions()) extends Actor {

  implicit val system: ActorSystem             = context.system
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  val logger                                   = LoggerFactory.getLogger(getClass)
  val debug = options.debug

  override def preStart() = {
    super.preStart()
    self ! GetPosition
  }

  def receive = {
    case GetPosition =>
      getMyPositionsTask.runToFuture.onComplete {
        case Success(value) =>
          if (debug) logger.debug(value.toString)
          value.fold(
            e => self ! InternalError(e.bodyString),
            r =>
              self ! PositionData(
                r.map(p => convertSizeToSignedValue(p.size, p.side)).sum
            )
          )
          Thread.sleep(options.positionInterval.toMillis)
          self ! GetPosition
        case Failure(exception) =>
          if (debug) logger.debug(exception.getMessage)
          self ! InternalError(exception.getMessage)
      }
    case v: PositionData =>
      if (debug) logger.debug(v.toString)
      context.parent ! v
    case InternalError(e) =>
      Thread.sleep(options.errorInterval.toMillis)
      throw new Exception(e)
    case other => logger.info(other.toString)
  }

  val getMyPositionsTask = {
    import scala.concurrent.ExecutionContextExecutor
    implicit val executionContext: ExecutionContextExecutor = system.dispatcher

    Task.fromFuture(
      api.getMyPositions.run(
        queryParameters = Some(
          QueryParameters(
            GetMyPositionsQueryParameters()
          )
        )
      )
    )
  }

  def convertSizeToSignedValue(size: BigDecimal, side: BitflyerEnumDefinition.Side): BigDecimal = {
    size *
    (side match {
      case BitflyerEnumDefinition.Side.Buy  => 1
      case BitflyerEnumDefinition.Side.Sell => -1
    })
  }

}

object GetMyFXStatus {

  case class GMFSOptions(
      positionInterval: Duration = 5.seconds,
      errorInterval: Duration = 5.seconds,
      debug: Boolean = false
  )

  sealed trait Command
  private case object GetPosition extends Command

  case class InternalError(errorMessage: String)

  sealed trait MyStatus
  case class PositionData(size: BigDecimal) extends MyStatus
  case class OrderData()                    extends MyStatus
  case class PnlData()                      extends MyStatus

}
