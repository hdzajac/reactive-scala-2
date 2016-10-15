package lab2

import akka.actor.{ActorRef, Props, ActorSystem}
import com.typesafe.config.ConfigFactory
import lab2.Seller._
import lab2.Buyer._

object AuctionApp extends App{
  val sellersNumber = 5
  val buyersNumber = 10
  val config = ConfigFactory.load()
  val system = ActorSystem("lab2", config.getConfig("auctionapp").withFallback(config))

  var i = 0

  system.actorOf(Props[AuctionSearchEngine], AuctionSearchEngine.ACTOR_NAME)
  system.actorOf(Props[Notifier], Notifier.ACTOR_NAME)

  val sellers: List[ActorRef] = for (i <- (1 to sellersNumber).toList) yield {
    val seller: ActorRef = system.actorOf(Props(new Seller()))
    seller ! Seller.Init
    seller
  }

  for (i <- 1 to buyersNumber) {
    val buyer = system.actorOf(Props(new Buyer(i)))
    buyer ! Buyer.Init
  }

  val auctionDispatcherSystem = ActorSystem("DispatcherSystem")
  auctionDispatcherSystem.actorOf(Props[AuctionDispatcher], AuctionDispatcher.ACTOR_NAME)

  system.awaitTermination()

}
