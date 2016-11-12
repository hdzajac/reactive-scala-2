import akka.actor.{Props, ActorRef, ActorSystem}
import akka.testkit.{TestProbe, ImplicitSender, TestKit}
import lab2.Auction
import lab2.Auction.{Bid, Start}
import org.joda.time.DateTime
import org.scalatest.{Matchers, BeforeAndAfterAll, WordSpecLike}
import scala.concurrent.duration._


class AuctionPersistenceTest1 extends TestKit(ActorSystem("auction_house_persistence_test")) with WordSpecLike with BeforeAndAfterAll with ImplicitSender with Matchers {

  override def afterAll(): Unit = {
    system.terminate
  }


  "An Auction" must {
    "resume in the same moment after crash" in {
      val auction5: ActorRef = system.actorOf(Props(classOf[Auction], DateTime.now().plusSeconds(15), "Phone5"))
      val seller8 = TestProbe("Seller6")
      val seller9 = TestProbe("Seller7")

      seller8.send(auction5,Start())
      seller9.send(auction5,Bid(150.0))
      seller9.expectMsg(5 seconds, Auction.BidAccepted)

    }
  }

}
