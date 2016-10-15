package lab2

import akka.actor.Actor.Receive
import akka.actor._
import akka.event.LoggingReceive
import akka.io.Udp.SimpleSender
import lab2.AuctionSearchEngine.AddNewAuction
import lab2.Notifier.Notify
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Random

object Auction {
  case class Start(product: String)

  case class Bid(price: Double) {
    require(price > 0)
  }

  case object BidAccepted

  case object BidRejected

  case object BidTimerExpired

  case object DeleteTimerExpired

  case object Relist

  case class Sold(productName: String, finalPrice: Double)
}


class Auction extends Actor{
  import context._
  import Auction._

  val random: Random = new Random()

  var product: String = _
  var bidTime: BigInt = _
  var seller: ActorRef = _
  var winner: ActorRef = _
  var highestPrice: Double = 0


  def receive: Receive = LoggingReceive{
    case Start(product) =>
      this.product = product
      this.seller = sender
      context.actorSelection(s"akka://lab2/user/${AuctionSearchEngine.ACTOR_NAME}") ! new AddNewAuction(product)
      context.system.scheduler.scheduleOnce(random.nextInt(10) seconds, self, BidTimerExpired)
      context become created

  }

  def handleBid(sender: ActorRef, price: Double) = {
    if (price > highestPrice) {
      log(f"Higher bid ($$$price%1.2f) accepted!")
      winner = sender
      highestPrice = price
      sender ! BidAccepted
      context.actorSelection(s"akka://lab2/user/${Notifier.ACTOR_NAME}") ! Notify(product, highestPrice, winner)
    }
    else {
      sender ! BidRejected
    }
  }

  def created(): Receive = LoggingReceive{
      case Bid(price) =>
        log("Created")
        handleBid(sender,price)
        context become activated

      case BidTimerExpired =>
        context.system.scheduler.scheduleOnce(5 seconds, self, DeleteTimerExpired)
        context become ignored

      case Relist =>
        context.system.scheduler.scheduleOnce(random.nextInt(15) seconds, self, BidTimerExpired)
  }

  def ignored (): Receive = LoggingReceive{
    case DeleteTimerExpired =>
      log("Finished, there were no buying offers")
      context.stop(self)

    case Bid(price) =>
      sender ! BidRejected

    case Relist =>
      log("Re-listing auction")
      context.system.scheduler.scheduleOnce(random.nextInt(10) seconds, self, BidTimerExpired)
      context become created
  }

  def activated (): Receive = LoggingReceive{
    case Bid(price) =>
      log("Received Bid")
      handleBid(sender,price)

    case BidTimerExpired =>
      context.system.scheduler.scheduleOnce(5 seconds, self, DeleteTimerExpired)
      context become sold

  }
  def sold (): Receive = LoggingReceive{
    case DeleteTimerExpired =>
      log("Finished, " + product + " sold for: " + highestPrice)
      winner ! Sold(product, highestPrice)
      seller ! Sold(product, highestPrice)
      context.stop(self)

    case Bid(price) =>
      sender ! BidRejected

  }

  def log(message: String): Unit = {
    println(f"[Auction ${product} ($$$highestPrice%1.2f)] $message")
  }
}
