package lab2

import akka.actor.{Actor, ActorRef, Props}
import akka.event.LoggingReceive
import akka.stream.FlowMonitorState.Finished
import lab2.Auction.Start
import lab2.AuctionSearch.{AddNewAuction, Added}
import org.joda.time.DateTime

import scala.util.Random
import scala.concurrent.duration.DurationInt


object Seller {

  case object Execute
  case class Execute(product: String)
  case object Init
  case class Init(product: String)
  case object GetWallet

  val items = List(
    "Apple IPhone 4",
    "Apple IPhone 4s",
    "Apple IPhone 5",
    "Apple IPhone 5s",
    "Apple IPhone 6",
    "Apple IPhone 6s",
    "Apple IPhone 7",
    "Samsung Galaxy S 2",
    "Samsung Galaxy S 3",
    "Samsung Galaxy S 4",
    "Samsung Galaxy S 5",
    "Samsung Galaxy S 6",
    "Samsung Galaxy S 7"
  )
}

class Seller extends Actor {
  import context._
  import Seller._

  val random: Random = new Random()

  var wallet: Double = 0
  var phone: String = _
  var testActor: ActorRef = _

  def log(message: String): Unit = {
    println(s"Selling phone: [${phone}]: ${message}")
  }

  override def receive: Actor.Receive = {
    case Init =>
      context.system.scheduler.scheduleOnce(random.nextInt(30) seconds, self, Execute)

    case Init(product) =>
      testActor = sender
       self ! Execute(product)

    case Execute =>
      phone = Seller.items(random.nextInt(Seller.items.size))
      val auction: ActorRef = context.actorOf(Props(classOf[Auction], DateTime.now().plusSeconds(15), phone))
      context.actorSelection(s"../${MasterSearch.ACTOR_NAME}") !  AddNewAuction(phone)
      log("started on random auction " + auction)
      auction ! Start()

    case Execute(product) =>
      phone = product
      //val auction: ActorRef = context.actorOf(Props(classOf[Auction], DateTime.now().plusSeconds(15), phone))
      context.actorSelection(s"../${MasterSearch.ACTOR_NAME}") !  AddNewAuction(phone)
      //auction ! Start()

    case Auction.Sold(productName, price) =>
      wallet += price
      println(f"[Seller: ${this}] $productName sold for $$$price%1.2f. Balance: $$$wallet%1.2f!")

    case GetWallet =>
      sender ! wallet

    case Added =>
      if(testActor != null){
        testActor ! Finished
      }
  }
}
