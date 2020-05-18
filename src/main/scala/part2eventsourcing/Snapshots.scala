package part2eventsourcing

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.{PersistentActor, SaveSnapshotFailure, SaveSnapshotSuccess, SnapshotOffer}

import scala.collection.mutable

object Snapshots extends App {

  case class ReceivedMessage(contents:String)
  case class SentMessage(contents:String)

  case class ReceivedMessageRecord(id: Int, contents:String)
  case class SentMessageRecord(id: Int, contents:String)

  class Chat(owner: String, contact: String) extends PersistentActor with ActorLogging{
    val MAX_MESSAGES = 10
    var currMessageID = 0
    val lastMessages = new mutable.Queue[(String, String)]

    var commandsWithoutCheckPoint = 0

    override def persistenceId: String = s"$owner-$contact-chat"

    override def receiveCommand: Receive = {
      case msg: ReceivedMessage =>
        persist(ReceivedMessageRecord(currMessageID, msg.contents)) {
          event => {
            log.info(s"Received Message ${event.contents}")
            maybeReplaceMessage(contact, event.contents)
            currMessageID +=1
            mayBeCheckPoint
          }
        }
      case msg: SentMessage =>
        persist(SentMessageRecord(currMessageID, msg.contents)) {
          event => {
            log.info(s"Sent Message ${event.contents}")
            maybeReplaceMessage(owner, event.contents)
            currMessageID +=1
            mayBeCheckPoint
          }
        }
      case "print" =>
        log.info(s"----------------------")
        lastMessages.foreach(x => log.info(x.toString))
        log.info(s"----------------------")

      case SaveSnapshotSuccess(metadata) =>
        log.info(s"Saving snapshotSuccess $metadata")

      case SaveSnapshotFailure(metadata, reason) =>
        log.warning(s"Failed $reason")
    }

    override def receiveRecover: Receive = {
      case msg: ReceivedMessageRecord => {
        log.info(s"recovered received Message $msg")
        maybeReplaceMessage(contact, msg.contents)
        currMessageID = msg.id
      }
      case msg: SentMessageRecord => {
        log.info(s"recovered sent Message $msg")
        maybeReplaceMessage(owner, msg.contents)
        currMessageID = msg.id
      }
      case SnapshotOffer(metadata, contents) => {
        log.info(s"recovered snapshot : $metadata")
        contents.asInstanceOf[mutable.Queue[(String, String)]].foreach(lastMessages.enqueue(_))
      } 
    }

    def maybeReplaceMessage(sender: String, contents: String) = {
      if(lastMessages.size > MAX_MESSAGES) {
        lastMessages.dequeue()
        lastMessages.enqueue((sender,contents))
      } else {
        lastMessages.enqueue((sender,contents))
      }
    }

    def mayBeCheckPoint = {
      commandsWithoutCheckPoint += 1
      if (commandsWithoutCheckPoint > MAX_MESSAGES) {
        log.info("Saving CheckPoint")
        saveSnapshot(lastMessages) //async operation
        commandsWithoutCheckPoint = 0
      }
    }
  }

  object Chat {
    def props(owner: String, contact: String) = Props(new Chat(owner, contact))
  }


  val system = ActorSystem("snapshots-demo")
  val chat = system.actorOf(Chat.props("ram", "siva"))

//  for(i <- 1 to 100000) {
//    chat ! SentMessage(s"Hi :D $i")
//    chat ! ReceivedMessage(s"Bye ! $i")
//  }

  chat ! "print"

}
