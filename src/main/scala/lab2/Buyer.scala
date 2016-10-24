package lab2

import akka.actor.Actor
import lab2.Auction.{NewHighestPrice, Sold, BidRejected, BidAccepted}
import lab2.AuctionSearch.GetAuctions
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.DurationInt
import scala.util.Random


object Buyer {
  case class Bid(amount: Int){
    require(amount > 0)
  }
  case class Notification (message: String)
  case object Init
  case object Execute

  val features = List("Iphone 4", "Samsung Galaxy", "Samsung", "Apple", "4", "7", "Iphone 6s", "5s")
}


class Buyer(val id: Integer) extends Actor{
  import context._
  import Buyer._

  val random: Random = new Random()
  val maxBid: Double = random.nextDouble() * 300
  val lookingFor = features(random.nextInt(features.size))
  var bid: Double = 0.0
  var bids = new ListBuffer[Double]()

  def log(message: String): Unit = {
    println(s"Buyer #${id} [${lookingFor}]: ${message}")
  }

  override def receive: Receive = {
    case Init =>
      log("Initialized! with max price: " + maxBid)
      waitABit
    case Execute =>
      context.actorSelection(s"../${AuctionSearch.ACTOR_NAME}") ! new GetAuctions(lookingFor)

    case AuctionSearch.SearchResult(auctions) =>
      if (auctions.nonEmpty) {
        val auction = auctions.toList(random.nextInt(auctions.size))
        bid = random.nextDouble() * maxBid
        log("bidding: " + bid + " on auction: " + auction)
        auction ! Auction.Bid(bid)
      }
      else {
        waitABit
      }
    case BidAccepted =>
      bids += bid
      waitABit
    case BidRejected =>
      waitABit
    case Sold(id, price) =>
      log(f"has bought the ${id} for $$$price%1.2f!")
    case NewHighestPrice(newHighest: Double) =>
      log("new highest price: " + newHighest)
      bids += newHighest
      if (newHighest + 1 <= maxBid){
        bid = newHighest + 1
        sender ! Auction.Bid(bid)
      }
  }

  def waitABit = {
    context.system.scheduler.scheduleOnce(random.nextInt(2) seconds, self, Execute)
  }

}
