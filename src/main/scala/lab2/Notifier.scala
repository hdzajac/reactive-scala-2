package lab2

import akka.actor.SupervisorStrategy.{Restart, Stop}
import akka.actor._

case class Notify(productName: String, buyer: ActorRef, price: BigDecimal)


class Notifier extends Actor {

  override val supervisorStrategy =
    OneForOneStrategy() {
      case _: IllegalStateException =>
        System.out.println("Notification exception...... Restarting")
        Restart
      case _: Exception =>
        System.out.println("Other exception encountered....... Quiting")
        Stop
    }

  override def receive: Receive = {
    case notification: Notify =>
      val notifierRequest: ActorRef = context.actorOf(Props(classOf[NotifierRequest], notification))
  }
}

