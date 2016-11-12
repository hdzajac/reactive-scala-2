package lab2

import akka.actor.SupervisorStrategy.{Restart, Stop}
import akka.actor._

case class Notify(productName: String, buyer: ActorRef, price: BigDecimal)


class Notifier extends Actor {

  override val supervisorStrategy =
    OneForOneStrategy() {
      case _: IllegalStateException =>
        System.out.println("Notification exception.")
        Restart
      case _: Exception => Stop
    }

  override def receive: Receive = {
    case notification: Notify => context.actorOf(Props(classOf[NotifierRequest], notification))
  }
}

