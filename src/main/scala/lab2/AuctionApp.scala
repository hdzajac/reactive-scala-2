package lab2

import akka.actor.{ActorRef, ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import lab2.Seller.Init

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object AuctionApp extends App{
  val sellersNumber = 5
  val buyersNumber = 10
  val config = ConfigFactory.load()
  val system = ActorSystem("AuctionHouse", config.getConfig("auction-house").withFallback(config))
  val publisher = ActorSystem("AuctionPublisher", config.getConfig("auction-publisher").withFallback(config))

  var i = 0

  val vector: Int = 2

  system.actorOf(Props(new MasterSearch(vector)), MasterSearch.ACTOR_NAME)

//
//  val sellers: List[ActorRef] = for (i <- (1 to sellersNumber).toList) yield {
//    val seller: ActorRef = system.actorOf(Props(new Seller()))
//    seller ! Seller.Init
//    seller
//  }
//
//  for (i <- 1 to buyersNumber) {
//    val buyer = system.actorOf(Props(new Buyer(i)))
//    buyer ! Buyer.Init
//  }
  val seller: ActorRef = system.actorOf(Props(new Seller()))
  system.actorOf(Props(new SupportTestActor(seller, vector))) ! Init

  system.actorOf(Props[Notifier], "notifier")
  publisher.actorOf(Props[AuctionPublisher], "auctionPublisher")

  Await.result(system.whenTerminated, Duration.Inf)
}
