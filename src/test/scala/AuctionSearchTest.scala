import akka.actor.{ ActorRef, Props, ActorSystem}
import akka.testkit.{TestProbe, ImplicitSender, TestKit}
import lab2.Auction.{DeleteTimerExpired, BidTimerExpired}
import lab2.AuctionSearch
import lab2.AuctionSearch.{SearchResult, GetAuctions, AddNewAuction}
import org.scalatest.{Matchers, BeforeAndAfterAll, WordSpecLike}
import scala.concurrent.duration._


class AuctionSearchTest extends TestKit(ActorSystem("auction_house")) with WordSpecLike with BeforeAndAfterAll with ImplicitSender with Matchers{

  override def afterAll(): Unit = {
    system.terminate
  }

  "A AuctionSearch" must{
    "save new auction and retrieve it" in {
      val auctionSearch = system.actorOf(Props[AuctionSearch], AuctionSearch.ACTOR_NAME)
      val auction = TestProbe("auction")
      val buyer = TestProbe("Buyer")
      auction.send(auctionSearch, AddNewAuction("Phone"))
      auction.send(auctionSearch, AddNewAuction("Phone 2"))

      buyer.send(auctionSearch, GetAuctions("Phone 2"))
      buyer.expectMsgPF(10 second) {
        case SearchResult(auctions) =>
          assert(auctions.toList.size == 1)
          assert(auctions.toList.contains(auction.ref))
        case _ =>
          assert(false)
      }
    }

    }

}

