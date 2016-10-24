import akka.actor.{Props, ActorRef, ActorSystem}
import akka.testkit.{TestProbe, ImplicitSender, TestKit}
import lab2.Buyer.Init
import lab2.{Buyer, Auction}
import lab2.Auction.{NewHighestPrice, Bid, Start}
import org.scalatest.{Matchers, BeforeAndAfterAll, WordSpecLike}
import scala.concurrent.duration._


class BuyerTest extends TestKit(ActorSystem("auction_house")) with WordSpecLike with BeforeAndAfterAll with ImplicitSender with Matchers {

  override def afterAll(): Unit = {
    system.terminate
  }

  "A Buyer" must {
    "bid if his bid was overbid" in {
      val buyer: ActorRef = system.actorOf(Props(new Buyer(1)))
      buyer ! Init

      buyer ! NewHighestPrice(2.0)

      expectMsg(2 seconds, Auction.Bid(3.0))
    }
  }

}
