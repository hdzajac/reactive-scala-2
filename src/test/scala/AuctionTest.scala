import akka.actor.{ActorRef, Props, ActorSystem}
import akka.testkit.{ImplicitSender, TestProbe, TestKit}
import com.typesafe.config.ConfigFactory
import lab2.Auction.{Bid, Start}
import lab2.{Auction, AuctionSearch}
import org.scalatest.{Matchers, BeforeAndAfterAll, WordSpecLike}
import scala.concurrent.duration._

class AuctionTest extends TestKit(ActorSystem("auction_house")) with WordSpecLike with BeforeAndAfterAll with ImplicitSender with Matchers {

  override def afterAll(): Unit = {
    system.terminate
  }



  "An Auction" must {
    "send info about accepting bid" in {
      val auction: ActorRef = system.actorOf(Props[Auction])
      val seller1 = TestProbe("seller1")
      seller1.send(auction, Start("Phone1"))
      seller1.send(auction, Bid(1000.0))

      seller1.expectMsg(20 second, Auction.BidAccepted)
    }

    "send info about rejecting bid" in {
      val auction: ActorRef = system.actorOf(Props[Auction])
      val seller2 = TestProbe("seller2")
      val seller3 = TestProbe("seller3")
      seller2.send(auction, Start("Phone2"))
      seller3.send(auction, Bid(1000.0))
      seller2.send(auction, Bid(1000.0))

      seller2.expectMsg(20 second, Auction.BidRejected)
    }

    "send info about won auction" in {
      val auction: ActorRef = system.actorOf(Props[Auction])
      val seller4 = TestProbe("seller4")
      val seller5 = TestProbe("seller5")
      seller4.send(auction, Start("Phone3"))
      seller5.send(auction, Bid(200.0))
      seller4.send(auction, Bid(1000.0))

      seller4.expectMsg(10 second, Auction.BidAccepted)
      seller4.expectMsg(50 second, Auction.Sold("Phone3", 1000.0))
    }

    "send info about new highest price" in {
      val auction: ActorRef = system.actorOf(Props[Auction])
      val seller6 = TestProbe("Seller6")
      val seller7 = TestProbe("Seller7")

      seller6.send(auction, Start("Phone4"))
      seller7.send(auction, Bid(200.0))
      seller6.send(auction, Bid(210.0))

      seller7.expectMsg(2 seconds, Auction.BidAccepted)
      seller7.expectMsg(5 seconds, Auction.NewHighestPrice(210.0))
    }
  }

}
