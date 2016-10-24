package lab2

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorRef, FSM}
import akka.event.LoggingReceive
import akka.io.Udp.SimpleSender
import lab2.AuctionSearch.AddNewAuction
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
final case class Initialized(price: Double, productName: String) extends Data
final case class WinningBuyer(winner: ActorRef, price: Double, productName: String) extends Data

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

  case class NewHighestPrice (newHighestPrice: Double)
}


class Auction extends FSM[State,Data]{
  import context._
  import Auction._

  val random: Random = new Random()
  var soldProduct:String = _
  var bidTime: BigInt = _

  startWith(InitState, Uninitialised)

  when(InitState) {
    case Event(Start(productName),Uninitialised) =>
      soldProduct = productName
      context.actorSelection(s"../../${AuctionSearch.ACTOR_NAME}") ! new AddNewAuction(productName)
      goto (Created) using Initialized(0,productName)
  }

  when(Created){
    case Event(Bid(price),d: Initialized) =>
      log("created")
      if(handleBid(sender, price, d.price)) {
        goto(Activated) using WinningBuyer(winner = sender, price = price, productName = d.productName )
      }
      else {
        stay()
      }
    case Event(BidTimerExpired, d: Initialized) =>
      log("Ignoring")
      goto(Ignored) using Initialized(d.price, d.productName)
  }

  when(Ignored){
    case Event(DeleteTimerExpired, d: Initialized) =>
      log("Deleting")
      stop()
    case Event(Bid(price),d :Initialized) =>
      sender ! BidRejected
      stay()
    case Event(Relist, d: Initialized) =>
      log("Re-listing")
      goto(Created) using Initialized(d.price, d.productName)
  }
  
  when(Activated) {
    case Event(Bid(price), d: WinningBuyer) =>
      log("received Bid from" + sender)
      if(handleBid(sender, price, d.price)) {
        if(sender != d.winner)
          d.winner ! NewHighestPrice(price)
        stay() using WinningBuyer(winner = sender,  price = price, productName = d.productName)
      }
      else {
        stay()
      }
    case Event(BidTimerExpired, d: WinningBuyer) =>
      log("Selling")
      goto(SoldState) using WinningBuyer(d.winner,d.price,d.productName)
  }
  
  when (SoldState) {
    case Event(DeleteTimerExpired, d: WinningBuyer) =>
      log("Finished, " + d.winner + " sold for: " + d.price)
      d.winner ! Sold(d.productName, d.price)
      parent ! Sold(d.productName, d.price)
      stop()
      
    case Event(Bid(price), _) =>
      sender ! BidRejected
      stay()
  }

  onTransition{
    case InitState -> Created =>
      context.system.scheduler.scheduleOnce(5  + random.nextInt(10)seconds , self, BidTimerExpired)

    case Created -> Ignored =>
      context.system.scheduler.scheduleOnce(5 seconds, self, DeleteTimerExpired)

    case Ignored -> Created =>
      context.system.scheduler.scheduleOnce(random.nextInt(10) seconds, self, BidTimerExpired)

    case Activated -> SoldState =>
      context.system.scheduler.scheduleOnce(5 seconds, self, DeleteTimerExpired)

  }


  def handleBid(sender: ActorRef, price: Double, highestPrice:Double):Boolean = {
    if (price > highestPrice) {
      log(f"Higher bid ($$$price%1.2f) accepted!")
      sender ! BidAccepted
      true
    }
    else {
      sender ! BidRejected
      false
    }
  }

  def log(message: String): Unit = {
    println(f"[Auction ${this}  $message")
  }
}
