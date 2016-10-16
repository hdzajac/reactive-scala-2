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
final case class Initialized(price: Double, seller: ActorRef, productName: String) extends Data
final case class WinningBuyer(winner: ActorRef, seller: ActorRef, price: Double, productName: String) extends Data

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
  var soldProduct:String = _
  var bidTime: BigInt = _

  startWith(InitState, Uninitialised)

  when(InitState) {
    case Event(Start(productName),Uninitialised) =>
      soldProduct = productName
      context.actorSelection(s"akka://lab2/user/${AuctionSearchEngine.ACTOR_NAME}") ! new AddNewAuction(productName)
      goto (Created) using Initialized(0,sender,productName)
  }

  when(Created){
    case Event(Bid(price),d: Initialized) =>
      log("created")
      if(handleBid(sender, price, d.price)) {
        context.actorSelection(s"akka://lab2/user/${Notifier.ACTOR_NAME}") ! Notify(d.productName, price, sender)
        goto(Activated) using WinningBuyer(winner = sender,seller = d.seller, price = price, productName = d.productName )
      }
      else {
        stay()
      }
    case Event(BidTimerExpired, d: Initialized) =>
      log("Ignoring")
      goto(Ignored) using Initialized(d.price, d.seller, d.productName)
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
      goto(Created) using Initialized(d.price,d.seller, d.productName)
  }
  
  when(Activated) {
    case Event(Bid(price), d: WinningBuyer) =>
      log("received Bid from" + sender)
      if(handleBid(sender, price, d.price)) {
        context.actorSelection(s"akka://lab2/user/${Notifier.ACTOR_NAME}") ! Notify(d.productName, price, sender)
        WinningBuyer(winner = sender, seller = d.seller, price = price, productName = d.productName)
        stay()
      }
      else {
        stay()
      }
    case Event(BidTimerExpired, d: WinningBuyer) =>
      log("Selling")
      goto(SoldState) using WinningBuyer(d.winner,d.seller,d.price,d.productName)
  }
  
  when (SoldState) {
    case Event(DeleteTimerExpired, d: WinningBuyer) =>
      log("Finished, " + d.winner + " sold for: " + d.price)
      d.winner ! Sold(d.productName, d.price)
      d.seller ! Sold(d.productName, d.price)
      stop()
      
    case Event(Bid(price), _) =>
      sender ! BidRejected
      stay()
  }

  onTransition{
    case InitState -> Created =>
      context.system.scheduler.scheduleOnce(random.nextInt(10) seconds, self, BidTimerExpired)

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
    println(f"[Auction ${soldProduct}  $message")
  }
}
