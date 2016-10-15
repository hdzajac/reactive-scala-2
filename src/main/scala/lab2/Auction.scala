package lab2

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorRef, FSM}
import akka.event.LoggingReceive
import akka.io.Udp.SimpleSender
import lab2.AuctionSearchEngine.AddNewAuction
import lab2.Notifier.Notify
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Random



sealed trait State

case object SoldState extends State
case object InitState extends State
case object Created extends State
case object Activated extends State
case object Ignored extends State

sealed trait Data

case object Uninitialised extends Data
final case class WinningBuyer(winner: ActorRef, price: Double) extends Data

object Auction {
  case class Start(productName: String)

  case class Bid(price: Double) {
    require(price > 0)
  }

  case object BidAccepted

  case object BidRejected

  case object BidTimerExpired

  case object DeleteTimerExpired

  case object Relist

  case class Sold(productName: String, finalPrice: Double)
}


class Auction extends FSM[State,Data]{
  import context._
  import Auction._

  val random: Random = new Random()

  var product: String = _
  var bidTime: BigInt = _
  var seller: ActorRef = _
  var winner: ActorRef = _
  var highestPrice: Double = 0

  startWith(InitState, Uninitialised)

  when(InitState) {
    case Event(Start(productName),Uninitialised) =>
      this.product = productName
      this.seller = sender
      goto (Created) using(WinningBuyer(null, 0))
  }

  when(Created){
    case Event(Bid(price),d: WinningBuyer) =>
      log("created")
      handleBid(sender, price)
      goto(Activated)
    case Event(BidTimerExpired, d: WinningBuyer) =>
      log("Ignoring")
      goto(Ignored)
  }

  when(Ignored){
    case Event(DeleteTimerExpired, d: WinningBuyer) =>
      log("Deleting")
      stop()
    case Event(Bid(price),d :WinningBuyer) =>
      sender ! BidRejected
      stay()
    case Event(Relist, d: WinningBuyer) =>
      log("Re-listing")
      goto(Created)
  }
  
  when(Activated) {
    case Event(Bid(price), d: WinningBuyer) =>
      log("received Bid from" + sender)
      handleBid(sender, price)
      stay()
    case Event(BidTimerExpired, d: WinningBuyer) =>
      log("Selling")
      goto(SoldState)
  }
  
  when (SoldState) {
    case Event(DeleteTimerExpired, d: WinningBuyer) =>
      log("Finished, " + product + " sold for: " + highestPrice)
      winner ! Sold(product, highestPrice)
      seller ! Sold(product, highestPrice)
      stop()
      
    case Event(Bid(price), _) =>
      sender ! BidRejected
      stay()
  }

  onTransition{
    case InitState -> Created =>
      context.actorSelection(s"akka://lab2/user/${AuctionSearchEngine.ACTOR_NAME}") ! new AddNewAuction(product)
      context.system.scheduler.scheduleOnce(random.nextInt(10) seconds, self, BidTimerExpired)

    case Created -> Ignored =>
      context.system.scheduler.scheduleOnce(5 seconds, self, DeleteTimerExpired)

    case Ignored -> Created =>
      context.system.scheduler.scheduleOnce(random.nextInt(10) seconds, self, BidTimerExpired)

    case Activated -> SoldState =>
      context.system.scheduler.scheduleOnce(5 seconds, self, DeleteTimerExpired)

  }


  def handleBid(sender: ActorRef, price: Double) = {
    if (price > highestPrice) {
      log(f"Higher bid ($$$price%1.2f) accepted!")
      winner = sender
      highestPrice = price
      sender ! BidAccepted
      context.actorSelection(s"akka://lab2/user/${Notifier.ACTOR_NAME}") ! Notify(product, highestPrice, winner)
    }
    else {
      sender ! BidRejected
    }
  }

  def log(message: String): Unit = {
    println(f"[Auction ${product} ($$$highestPrice%1.2f)] $message")
  }
}
