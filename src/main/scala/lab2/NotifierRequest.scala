package lab2

import akka.actor.{Actor}
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._

import scala.concurrent.Await


class NotifierRequest(notification: Notify) extends Actor {

  implicit val timeout = Timeout(3 seconds)

  override def preStart(): Unit = {
    self ! notification
  }

  override def receive: Receive = {
    case n: Notify =>
      val future = context.actorSelection("akka.tcp://AuctionPublisher@127.0.0.1:2553/user/auctionPublisher") ? n
      Await.result(future, timeout.duration)
  }
}

