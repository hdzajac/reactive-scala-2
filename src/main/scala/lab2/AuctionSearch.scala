package lab2

import akka.actor.{Terminated, Actor, ActorRef}
import scala.collection.mutable
import scala.util.Random


object AuctionSearch {

  val ACTOR_NAME = "AUCTION_SEARCH"

  case object Added
  case class GetAuctions(reference: String)
  case class AddNewAuction(product: String)
  case class SearchResult(auctions: Iterable[ActorRef])
}

class AuctionSearch extends Actor{
  import AuctionSearch._

  val random: Random = new Random()

  var auctions: mutable.Map[String, ActorRef] = mutable.Map()

  override def receive: Actor.Receive = {
    case AddNewAuction(productName) =>
      auctions += (productName.toLowerCase() -> sender)
      context.watch(sender)
      sender ! Added

    case GetAuctions(reference) =>
      val found = auctions.filterKeys(_.contains(reference.toLowerCase))
      sender !  SearchResult(found.values)

    case Terminated(auction) =>
      auctions.retain((name, a) => a.equals(auction))
  }
}
