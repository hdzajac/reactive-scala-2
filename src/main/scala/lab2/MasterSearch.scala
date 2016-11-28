package lab2

import akka.actor.{Actor, ActorLogging, Props}
import akka.event.LoggingReceive
import akka.routing.{ActorRefRoutee, BroadcastRoutingLogic, RoundRobinRoutingLogic, Router}
import lab2.AuctionSearch.{AddNewAuction, GetAuctions}

object MasterSearch{
  val ACTOR_NAME = "MASTER_SEARCH"
}

class MasterSearch(number: Int) extends Actor with ActorLogging {


  val routees = Vector.fill(number) {
    val r = context.actorOf(Props[AuctionSearch])
    context watch r
    ActorRefRoutee(r)
  }

  val registerRouter = Router(BroadcastRoutingLogic(), routees)

  val searchRouter = Router(RoundRobinRoutingLogic(), routees)

  override def receive: Receive =  {
    case auction: AddNewAuction =>
      //System.out.println("Registering new auction in Master " + auction.product)
      registerRouter.route(auction, sender())
    case token: GetAuctions =>
      searchRouter.route(token, sender())
      //System.out.println("Searching for new auction in Master " + token.reference)
  }
}
