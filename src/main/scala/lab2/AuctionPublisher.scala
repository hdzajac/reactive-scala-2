package lab2

import akka.actor.{Actor, Status}

class AuctionPublisher extends Actor{

  private val MaxCounter: Int = 3
  private var counter = 0

  // Fails every 3rd time
  override def receive: Receive = {
    case n: Notify =>
      if (counter < MaxCounter) {
        System.out.println(s"Received notification: [" + n.productName + ", "+ n.buyer + ", " +  n.price + "].")
        counter += 1
        sender() ! Status.Success
      } else {
        System.out.println("Notification error for: [" + n.productName + ", "+ n.buyer + ", " +  n.price + "].")
        counter = 0
        sender() ! Status.Failure(new IllegalStateException())
      }
  }
}

