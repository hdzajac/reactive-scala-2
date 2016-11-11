import akka.actor.{ActorRef, Props, ActorSystem}
import akka.testkit.{ImplicitSender, TestProbe, TestKit}
import com.typesafe.config.ConfigFactory
import lab2.Auction.{Bid, Start}
import lab2.{Auction, AuctionSearch}
import org.joda.time.DateTime
import org.scalatest.{Matchers, BeforeAndAfterAll, WordSpecLike}
import scala.concurrent.duration._

class AuctionTest extends TestKit(ActorSystem("auction_house_test")) with WordSpecLike with BeforeAndAfterAll with ImplicitSender with Matchers {

  override def afterAll(): Unit = {
    system.terminate
  }



  "An Auction" must {
    "send info about accepting bid" in {
      val auction1: ActorRef = system.actorOf(Props(classOf[Auction], DateTime.now().plusSeconds(15), "Phone1"))
      val seller1 = TestProbe("seller1")
      seller1.send(auction1, Start())
      seller1.send(auction1, Bid(1000.0))

      seller1.expectMsg(20 second, Auction.BidAccepted)
    }

    "send info about rejecting bid" in {
      val auction2: ActorRef = system.actorOf(Props(classOf[Auction], DateTime.now().plusSeconds(15), "Phone2"))
      val seller2 = TestProbe("seller2")
      val seller3 = TestProbe("seller3")
      seller2.send(auction2, Start())
      seller3.send(auction2, Bid(1000.0))
      seller2.send(auction2, Bid(1000.0))

      seller2.expectMsg(20 second, Auction.BidRejected)
    }

    "send info about won auction" in {
      val auction3: ActorRef = system.actorOf(Props(classOf[Auction], DateTime.now().plusSeconds(15), "Phone3"))
      val seller4 = TestProbe("seller4")
      val seller5 = TestProbe("seller5")
      seller4.send(auction3, Start())
      seller5.send(auction3, Bid(200.0))
      seller4.send(auction3, Bid(1000.0))

      seller4.expectMsg(10 second, Auction.BidAccepted)
      seller4.expectMsg(50 second, Auction.Sold("Phone3", 1000.0))
    }

    "send info about new highest price" in {
      val auction4: ActorRef = system.actorOf( Props(classOf[Auction], DateTime.now().plusSeconds(15), "Phone4"))
      val seller6 = TestProbe("Seller6")
      val seller7 = TestProbe("Seller7")

      seller6.send(auction4, Start())
      seller7.send(auction4, Bid(200.0))
      seller6.send(auction4, Bid(210.0))

      seller7.expectMsg(2 seconds, Auction.BidAccepted)
      seller7.expectMsg(5 seconds, Auction.NewHighestPrice(210.0))
    }
  }

}
