package lab2

import akka.actor.Actor
import lab2.Auction.{Sold, BidRejected, BidAccepted}
import lab2.AuctionSearchEngine.GetAuctions
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
  val lookingFor = features(random.nextInt(features.size))
  var bid: Double = 0.0

  def log(message: String): Unit = {
    println(s"Buyer #${id} [${lookingFor}]: ${message}")
  }

  override def receive: Receive = {
    case Init =>
      log("Initialized!")
      waitABit
    case Execute =>
      context.actorSelection(s"akka://lab2/user/${AuctionSearchEngine.ACTOR_NAME}") ! new GetAuctions(lookingFor)

    case AuctionSearchEngine.SearchResult(auctions) =>
      if (auctions.nonEmpty) {
        val auction = auctions.toList(random.nextInt(auctions.size))
        log("bidding on auction: " + auction)
        bid = random.nextDouble() * 100.0
        auction ! Auction.Bid(bid)
      }
      else {
        waitABit
      }
    case BidAccepted =>
      waitABit
    case BidRejected =>
      waitABit
    case Sold(id, price) =>
      log(f"has bought the ${id} for $$$price%1.2f!")
      context.stop(self)
  }

  def waitABit = {
    context.system.scheduler.scheduleOnce(random.nextInt(2) seconds, self, Execute)
  }

}
