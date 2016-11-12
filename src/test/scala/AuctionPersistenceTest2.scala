import akka.actor.{Props, ActorRef, ActorSystem}
import akka.testkit.{TestProbe, ImplicitSender, TestKit}
import lab2.Auction
import lab2.Auction.{Bid, Start}
import org.joda.time.DateTime
import org.scalatest.{Matchers, BeforeAndAfterAll, WordSpecLike}
import scala.concurrent.duration._


class AuctionPersistenceTest2 extends TestKit(ActorSystem("auction_house_persistence_test")) with WordSpecLike with BeforeAndAfterAll with ImplicitSender with Matchers {

  "A previously created auction" must {
    "be initialised with AuctionPersistenceTest1 state" in {
      val auction5: ActorRef = system.actorOf(Props(classOf[Auction], DateTime.now().plusSeconds(15), "Phone5"))
      val seller9 = TestProbe("Seller7")

      seller9.send(auction5,Bid(75.0))
      seller9.expectMsg(10 seconds, Auction.BidRejected)


    }
  }
}
