package com.github.BambooTuna.BFPackage

import akka.actor.{Actor, ActorSystem, OneForOneStrategy, Props}
import akka.actor.SupervisorStrategy.Restart
import akka.stream.ActorMaterializer
import com.github.BambooTuna.BFPackage.EnumDefinition.StreamChannel
import com.github.BambooTuna.BFPackage.StreamActor.LightningExecutions
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

object StreamSampleRoot extends App {

  implicit val system: ActorSystem                        = ActorSystem("StreamSampleRoot")
  implicit val materializer: ActorMaterializer            = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  val logger                                              = LoggerFactory.getLogger(getClass)

  system.actorOf(Props(classOf[StreamSampleActor]), "StreamSampleActor")

}

class StreamSampleActor extends Actor {

  implicit val system: ActorSystem                        = context.system
  implicit val materializer: ActorMaterializer            = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  val logger                                              = LoggerFactory.getLogger(getClass)

  val executionsSpot =
    context.actorOf(Props(classOf[StreamActor], StreamChannel.Executions_Spot), "Executions_Spot")

  def receive = {
    case LightningExecutions(list) =>
      logger.info(list.toString())
    case other =>
      logger.debug(other.toString)
  }

  override def supervisorStrategy = OneForOneStrategy() {
    case _ =>
      Thread.sleep(5.seconds.toMillis)
      Restart
  }

}
