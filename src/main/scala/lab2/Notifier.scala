package lab2

import akka.actor.{Actor, ActorRef}

object Notifier {
  val ACTOR_NAME = "notifier"

  case class Notify(product: String, price: Double, winner: ActorRef)
}

class Notifier extends Actor {
  import Notifier._

  val auctionDispatcher = context.system.actorSelection(s"akka://lab2/user/${AuctionDispatcher.ACTOR_NAME}")

  override def receive: Receive = {
    case Notify(product, price, winner) =>
      auctionDispatcher ! Notify(product, price, winner)
  }

}