package lab2

import akka.actor.{Actor, ActorRef, ActorSelection, Props}
import akka.event.LoggingReceive
import akka.stream.FlowMonitorState.Finished
import lab2.AuctionSearch.{AddNewAuction, GetAuctions, SearchResult}
import lab2.Seller.Init
import org.joda.time.DateTime
import sun.management.ManagementFactoryHelper.LoggingMXBean

import scala.util.Random

class SupportTestActor(seller: ActorRef, vectorSize: Int) extends Actor{
  val random:Random = new Random()
  val master: ActorSelection = context.actorSelection(s"../${MasterSearch.ACTOR_NAME}")
  var counter:Int = 0
  var initialized:Int = 1
  var initiailzer: ActorRef = _
  val auctionsNumber:Int = 50000
  val searchesNumber:Int = 10000


  override def receive: Receive = LoggingReceive {
    case Init =>
      initiailzer = sender
      log("Before registering")
      seller ! Init("Product0")

    case Finished =>
      if(initialized == auctionsNumber*vectorSize) {
        context become searching
        log("Finished adding")
        self ! Init
      }
      else {
        if(initialized % vectorSize == 0) {
          seller ! Init("Product" + initialized)
        }
        initialized += 1
      }
  }

  def searching: Receive = LoggingReceive {
    case Init =>
      log("Beginning of search")
      master ! GetAuctions("Product0")

    case AuctionSearch.SearchResult(auctions) =>
      counter += 1
      if(counter == searchesNumber) {
        log("Fetched all results")
      } else if (counter % 1000 == 0){
        //log(counter + " messages received")
        master ! GetAuctions("Product"+counter)
      }
      else master ! GetAuctions("Product" + counter)
  }

  def log(msg: String): Unit ={
    System.out.println(String.format("[%s] [%s]",msg , DateTime.now().toString("H:m:ss SSS")))
  }
}
