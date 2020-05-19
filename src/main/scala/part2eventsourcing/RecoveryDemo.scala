package part2eventsourcing

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.{PersistentActor, Recovery, RecoveryCompleted, SnapshotSelectionCriteria}

object RecoveryDemo extends App {


  case class Command(contents: String)
  case class Event(contents: String)
  class RecoveryActor extends PersistentActor with ActorLogging {

    override def persistenceId: String = "recovery-actor"

    override def receiveCommand: Receive = {
      case Command(contents) =>
        persist(Event(contents)) { event =>
          log.info(s"Successfully persisted $event")
        }

    }

    override def receiveRecover: Receive = {
      case Event(contents) =>
        log.info(s"Recovered $contents")
      case RecoveryCompleted =>
        log.info(s"Recovery completed !!!")
    }

    override def onRecoveryFailure(cause: Throwable, event: Option[Any]): Unit = {
      log.error(s"$cause -> $event")
    }


    //override def recovery: Recovery = Recovery(fromSnapshot = SnapshotSelectionCriteria.Latest)

    //recovery wont be done
    //override def recovery: Recovery = Recovery.none
  }

  val system = ActorSystem("recovery-demo")

  val recoveryActor = system.actorOf(Props[RecoveryActor], "recoveryActor")

  //ALL the commands sent during recovery are stashed
  for (i <- 1 to 1000)
    recoveryActor ! Command(s"contents$i")

  //if the actor fails during recovery, actor is stopped

  //customize recovery to recover n messages
}
