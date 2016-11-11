package lab2

import java.util.concurrent.TimeUnit

import akka.actor.ActorRef
import akka.event.LoggingReceive
import akka.persistence.{SaveSnapshotSuccess, PersistentActor, SnapshotOffer}
import lab2.Auction.{NewHighestPrice}
import lab2.AuctionSearch.AddNewAuction
import org.joda.time.{Seconds, DateTime}

import scala.concurrent.duration._
import scala.util.Random
import scala.reflect._

//States
sealed trait State

case object Created extends State
case object Ignored extends State
case object Activated extends State
case object SoldState extends State


//Events
case class StateChangeEvent(state: State)
case class BidEvent(sender: ActorRef, price: Double)
case class InitEvent()



object Auction {
  //Messages
  case class Start()
  case class Bid(price: Double) {
    require(price > 0)
  }
  case object BidAccepted
  case object BidRejected
  case object BidTimerExpired
  case object DeleteTimerExpired
  case class Sold(productName: String, finalPrice: Double)
  case class NewHighestPrice (newHighestPrice: Double)

  def log(message: String): Unit = {
    println(f"[Auction ${this}  $message")
  }

  def handleBid(sender: ActorRef, p: Double, cp: Double):Boolean = {
    if (p > cp) {
      log(f"Higher bid ($$$p%1.2f) accepted!")
      sender ! BidAccepted
      true
    }
    else {
      sender ! BidRejected
      false
    }
  }
}

//Data
case class AuctionData( soldProduct: String, currentPrice: Double = 0.0, currentWinner: ActorRef = null){
  def updated(evt: BidEvent): AuctionData = {
    println(s"Applying $evt: price $currentPrice for: $soldProduct")
    if(Auction.handleBid(evt.sender, evt.price, currentPrice)) {
      if (currentWinner != null && evt.sender != currentWinner) {
        currentWinner ! NewHighestPrice(evt.price)
      }
      AuctionData(soldProduct, evt.price, evt.sender)
    }
    else
      this
  }

  override def toString: String = "Sold: " + soldProduct + " for: " + currentPrice + " to: " + currentWinner
}




class Auction(finishTime: DateTime, soldProduct: String) extends PersistentActor{
  import Auction._
  import context._

  override def persistenceId = "persistent-auction-id-" + this.hashCode()

  var data: AuctionData = AuctionData(soldProduct)

  def startBidTimer(duration: Duration) = context.system.scheduler.scheduleOnce(
    FiniteDuration(duration toSeconds, TimeUnit.SECONDS), self, BidTimerExpired)

  def setTimer() = {
    val currentTime: DateTime = DateTime.now()
    if (currentTime.isBefore(finishTime)){
      val duration:Duration = Duration.create(Seconds.secondsBetween(currentTime,finishTime).toStandardDuration.getStandardSeconds, TimeUnit.SECONDS)
      startBidTimer(duration)
    }
  }

  def updateState(event: StateChangeEvent): Unit = {
    println(s"Applying $event")
    context.become(event.state match {
      case Created =>
        setTimer()
        created
      case Activated =>
        activated
      case Ignored =>
        context.system.scheduler.scheduleOnce(5 seconds, self, DeleteTimerExpired)
        ignored
      case SoldState =>
        context.system.scheduler.scheduleOnce(5 seconds, self, DeleteTimerExpired)
        soldState
    })
  }

  def updateState(event: BidEvent): Unit = {
    data = data.updated(event)
    saveSnapshot(data)
  }

  def updateState(event: InitEvent): Unit = {
    context.actorSelection(s"../../${AuctionSearch.ACTOR_NAME}") ! new AddNewAuction(soldProduct)
  }


  def initState: Receive =  {
      case Start() =>
        persist(InitEvent()){
          event => updateState(event)
        }
        persist(StateChangeEvent(Created)){
          event => updateState(event)
        }
  }

  def created: Receive =  {
      case Bid(price) =>
        persist(BidEvent(sender,price)){
          event => updateState(event)
        }
        persist(StateChangeEvent(Activated)){
          event =>
            updateState(event)
        }

      case BidTimerExpired =>
        persist(StateChangeEvent(Ignored)){
          event => updateState(event)
        }
    }

    def ignored: Receive = LoggingReceive {
      case DeleteTimerExpired =>
        context.stop(self)

      case Bid(price) =>
        sender ! BidRejected

    }

    def activated: Receive = {
      case Bid(price) =>
        persist(BidEvent(sender,price)){
          event => updateState(event)
        }

      case BidTimerExpired =>
        persist(StateChangeEvent(SoldState)){
          event => updateState(event)
        }
    }

    def soldState: Receive = LoggingReceive {
      case DeleteTimerExpired =>
        data.currentWinner ! Sold(data.soldProduct, data.currentPrice)
        parent ! Sold(data.soldProduct, data.currentPrice)
        context.stop(self)

      case Bid(price) =>
        sender ! BidRejected
    }

  override def receiveCommand: Receive = initState

  override def receiveRecover: Receive = {
    case evt: StateChangeEvent => updateState(evt)
    case SnapshotOffer(metadata, snapshot: AuctionData) => {
      log(s"offer, AuctionData: $snapshot, metadata: $metadata")
      data = snapshot
    }
  }

}
