package part2eventsourcing

import java.util.Date

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.PersistentActor

object PersistentActors extends App {

  //COMMANDS
  case class Invoice(recipient: String, date: Date, amount: Int)
  //EVENTS
  case class InvoiceRecorded(id: Int, recipient: String, date: Date, amount: Int)

  var latestInvoiceID = 0
  var totalAmount = 0

  class Accountant extends PersistentActor with ActorLogging {

    override def persistenceId: String = "simpleaccount"


    //receive handler for the actor
    override def receiveCommand: Receive = {
      case Invoice(recipient: String, date: Date, amount: Int) =>
        log.info(s"received invoice for $amount")
        //persist and handling are done asynchronously
        persist(InvoiceRecorded(latestInvoiceID, recipient, date, amount)) /*all other messages are stashed between these two*/ { e =>
          //akka persistence ensures that no other thread access this code
          //safe to access mutable/shared resources here
          latestInvoiceID += 1
          totalAmount += amount
          log.info(s"persisted ${e.id}")
        }
      case "print" =>
        log.info("latest info")
        sender() ! "print executed"
    }

    //handler called on recovery
    override def receiveRecover: Receive = {
      case InvoiceRecorded(id, _, _, amount) => {
        latestInvoiceID = id
        totalAmount += amount
        log.info(s"recovered invoice $latestInvoiceID for amount $totalAmount")
      }
    }
  }

  val system = ActorSystem("persistent-actors")
  val accountant = system.actorOf(Props[Accountant], "simpleAccountant")

//  for (i <- 1 to 10) {
//    accountant ! Invoice("The safe company", new Date, 1000)
//  }
}
