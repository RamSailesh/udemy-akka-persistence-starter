package part3storesandserialization

import akka.actor.ActorLogging
import akka.persistence.{PersistentActor, RecoveryCompleted, SaveSnapshotFailure, SaveSnapshotSuccess, SnapshotOffer}

class SimplePersistentActor extends PersistentActor with ActorLogging {
  override def persistenceId: String = "simple-persistent-actor"

  var nMessages= 0

  override def receiveRecover: Receive = {
    case SnapshotOffer(metadata, payload:Int) =>
      log.info("recovered snapshot")
      nMessages = payload
    case RecoveryCompleted => log.info("recovery done")
    case message =>
      log.info(s"recivered $message")
      nMessages += 1
  }

  override def receiveCommand: Receive = {
    case "print" =>
      log.info(s"I've persisted $nMessages so far")
    case "snap" =>
      saveSnapshot(nMessages)
    case message =>
      persist(message) {
        e => {
          log.info(s" i've persisted $e")
          nMessages += 1
        }
      }

    case SaveSnapshotSuccess(metadata) =>
      log.info(s"Saving snapshotSuccess $metadata")

    case SaveSnapshotFailure(metadata, reason) =>
      log.warning(s"Saving Snapshot Failed $reason")
  }
}