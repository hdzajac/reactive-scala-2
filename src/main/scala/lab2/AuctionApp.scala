package lab2

import akka.actor.{ActorRef, Props, ActorSystem}
import com.typesafe.config.ConfigFactory
import lab2.Seller._
import lab2.Buyer._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object AuctionApp extends App{
  val sellersNumber = 5
  val buyersNumber = 10
  val config = ConfigFactory.load()
  val system = ActorSystem("auction_house")

  var i = 0

  system.actorOf(Props[AuctionSearch], AuctionSearch.ACTOR_NAME)

  val sellers: List[ActorRef] = for (i <- (1 to sellersNumber).toList) yield {
    val seller: ActorRef = system.actorOf(Props(new Seller()))
    seller ! Seller.Init
    seller
  }

  for (i <- 1 to buyersNumber) {
    val buyer = system.actorOf(Props(new Buyer(i)))
    buyer ! Buyer.Init
  }


  Await.result(system.whenTerminated, Duration.Inf)
}
