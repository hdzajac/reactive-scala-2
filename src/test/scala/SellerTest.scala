import akka.actor.{Props, ActorRef, ActorSystem}
import akka.testkit.{TestProbe, ImplicitSender, TestKit}
import lab2.Auction.{Start, BidAccepted, Sold, Bid}
import lab2.AuctionSearch.{SearchResult, GetAuctions}
import lab2.{Auction, AuctionSearch, Seller}
import lab2.Seller.{GetWallet, Init}
import org.scalatest.{Matchers, BeforeAndAfterAll, WordSpecLike}
import scala.concurrent.duration._


class SellerTest extends TestKit(ActorSystem("auction_house")) with WordSpecLike with BeforeAndAfterAll with ImplicitSender with Matchers{

  override def afterAll(): Unit = {
    system.terminate
  }

  val auctionSearch = system.actorOf(Props[AuctionSearch], AuctionSearch.ACTOR_NAME)


  "A Seller " must {
    "start an auction" in {
      val seller:ActorRef = system.actorOf(Props[Seller], "Seller")

      seller ! Init("Phone")

      val buyer = TestProbe("buyer")
      var auction: ActorRef = null

      Thread.sleep(2000)

      buyer.send(auctionSearch, GetAuctions("Phone"))

      buyer.expectMsgPF(10 second){
        case SearchResult(auctions) =>
          assert(auctions.toList.size == 1)
          auction = auctions.toList(0)
        case _ =>
          assert(false)
      }
    }

    "has in wallet money for sold item" in {

      val seller2:ActorRef = system.actorOf(Props[Seller], "Seller2")

      seller2 ! Init("Phone2")

      val buyer2 = TestProbe("buyer2")
      var auction2: ActorRef = null

      buyer2.send(auctionSearch, GetAuctions("Phone2"))

      buyer2.expectMsgPF(10 second){
        case SearchResult(auctions) =>
          assert(auctions.toList.size == 1)
          auction2 = auctions.toList(0)
        case _ =>
          assert(false)
      }
      buyer2.send(auction2, Bid(1000.0))
      buyer2.expectMsg(10 second, BidAccepted)
      buyer2.expectMsg(35 second, Sold("Phone2", 1000.0))

      seller2 ! GetWallet
      expectMsg(1000.0)
    }

    "has an auction as child" in {
      val sellerParent = TestProbe("seller4")
      val auction: ActorRef = sellerParent.childActorOf(Props[Auction])
      val seller5 = TestProbe("seller5")
      sellerParent.send(auction, Start("Phone3"))
      seller5.send(auction, Bid(1000.0))

      sellerParent.expectMsg(30 second, Auction.Sold("Phone3", 1000.0))
    }
  }
}
