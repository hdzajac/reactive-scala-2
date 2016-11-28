import akka.actor.{ActorRef, ActorSelection, ActorSystem, Props}
import akka.event.LoggingReceive
import akka.stream.FlowMonitorState.Finished
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import lab2.AuctionSearch.{GetAuctions, SearchResult}
import lab2.{MasterSearch, Seller, SupportTestActor}
import lab2.Seller.Init

import scala.concurrent.duration._
import org.joda.time.DateTime
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class RoutingTest extends TestKit(ActorSystem("auction_house")) with WordSpecLike with BeforeAndAfterAll with ImplicitSender with Matchers{

  override def afterAll(): Unit = {
    system.terminate
  }

  "A Support Test Actor" should {
    "execute scenario" in {
      system.actorOf(Props[MasterSearch], MasterSearch.ACTOR_NAME)
      system.actorOf(Props[SupportTestActor], "support_actor") ! Init
      expectMsg(60 seconds, Finished)
    }
  }
}
