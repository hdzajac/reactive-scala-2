package lab2

import akka.actor.{Props, ActorRef, Actor}
import lab2.Auction.Start
import scala.util.Random
import scala.concurrent.duration.DurationInt


object Seller {

  case object Execute
  case object Init

  val items = List(
    "Apple IPhone 4",
    "Apple IPhone 4s",
    "Apple IPhone 5",
    "Apple IPhone 5s",
    "Apple IPhone 6",
    "Apple IPhone 6s",
    "Apple IPhone 7",
    "Samsung Galaxy S 2",
    "Samsung Galaxy S 3",
    "Samsung Galaxy S 4",
    "Samsung Galaxy S 5",
    "Samsung Galaxy S 6",
    "Samsung Galaxy S 7"
  )
}

class Seller extends Actor {
  import context._
  import Seller._

  val random: Random = new Random()

  var wallet: Double = 0
  var phone: String = _

  def log(message: String): Unit = {
    println(s"Selling phone: [${phone}]: ${message}")
  }

  override def receive: Actor.Receive = {
    case Init =>
      context.system.scheduler.scheduleOnce(random.nextInt(30) seconds, self, Execute)

    case Execute =>
      phone = Seller.items(random.nextInt(Seller.items.size))
      val auction: ActorRef = context.system.actorOf(Props[Auction])
      log("started on auction " + auction)
      auction ! Start(phone)

    case Auction.Sold(productName, price) =>
      wallet += price
      println(f"$productName sold for $$$price%1.2f. Balance: $$$wallet%1.2f!")
      context.stop(self)
  }
}
