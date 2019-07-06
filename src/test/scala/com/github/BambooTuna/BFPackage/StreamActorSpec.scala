package com.github.BambooTuna.BFPackage

import akka.actor.{Actor, ActorSystem, OneForOneStrategy, Props}
import akka.actor.SupervisorStrategy.Restart
import akka.stream.ActorMaterializer
import com.github.BambooTuna.BFPackage.EnumDefinition.StreamChannel
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

object StreamActorRootSpec extends App {

  implicit val system: ActorSystem                        = ActorSystem("StreamActorRootSpec")
  implicit val materializer: ActorMaterializer            = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  val logger                                              = LoggerFactory.getLogger(getClass)

  system.actorOf(Props(classOf[StreamActorSpec]), "StreamActorSpec")

}

class StreamActorSpec extends Actor {

  implicit val system: ActorSystem                        = context.system
  implicit val materializer: ActorMaterializer            = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  val logger                                              = LoggerFactory.getLogger(getClass)

  val executionsSpot =
    context.actorOf(Props(classOf[StreamActor], StreamChannel.Executions_Spot), "Executions_Spot")

  def receive = {
    case other => logger.info(other.toString)
  }

  override def supervisorStrategy = OneForOneStrategy() {
    case _ =>
      Thread.sleep(5.seconds.toMillis)
      Restart
  }

}
