package lab2

import akka.actor.Actor
import lab2.Notifier.Notify

object AuctionDispatcher {
  val ACTOR_NAME = "AUCTION_DISPATCHER"
}

class AuctionDispatcher extends Actor {
  override def receive: Receive = {
    case Notify(product, price, winner) =>
      println(s"NEW AUCTION STATE PUBLISHED: $product will be sold for $price!")
  }
}

