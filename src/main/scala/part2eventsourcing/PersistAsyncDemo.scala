package part2eventsourcing

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.persistence.PersistentActor

object PersistAsyncDemo extends App {


  case class Command (contents: String)
  case class Event(contents: String)

  object CriticalStreamProcessor {
    def props(eventAggregator: ActorRef) = Props(new CriticalStreamProcessor(eventAggregator))
  }

  class CriticalStreamProcessor(eventAggregator: ActorRef) extends PersistentActor with ActorLogging {
    override def persistenceId: String = "critical-stream-processor"

    override def receiveCommand: Receive = {
      case Command(contents) =>
        eventAggregator ! s"Processing Contents $contents"
        persistAsync(Event(contents)){
          event => {
            eventAggregator ! event
          }
        }
        val processedEvents = contents + "_ proessed"
        persistAsync(Event(processedEvents)) {
          e => {
            eventAggregator ! e
          }
        }
    }

    override def receiveRecover: Receive = {
      case message => log.info(s"Recovered $message")
    }
  }

  class EventAggregator extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(s"Aggregating $message")
    }
  }

  val system = ActorSystem("Persist-Async-Demo")

  val eventAggregator = system.actorOf(Props[EventAggregator])
  val processor = system.actorOf(CriticalStreamProcessor.props(eventAggregator))

  processor ! Command("command1")
  processor ! Command("command2")

  /*
  PersistAsync has upper hand on performance -> Thoroughput
  in Persist we have to wait until event is processed.

  persist - guarantees everything in order
  persistAsync - guarantees calls and callbacks in order, any mutation blocks present within the code may put the actor in non-deterministic state
   */

}
