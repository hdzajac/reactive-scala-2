package lab2

import akka.actor.{Actor, Status}

class AuctionPublisher extends Actor {

  private val MaxCounter: Int = 3

  private var counter = 0

  override def receive: Receive = {
    case n: Notify => {
      System.out.println(s"Received notification: [" + n.productName + ", " + n.buyer + ", " + n.price + "].")
      sender() ! Status.Success
    }
  }
}

