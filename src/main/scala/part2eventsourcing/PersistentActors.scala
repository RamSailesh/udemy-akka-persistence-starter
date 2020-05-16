package part2eventsourcing

import java.util.Date

import akka.actor.{ActorContext, ActorLogging, ActorSystem, Props}
import akka.persistence.PersistentActor

object PersistentActors extends App {

  //COMMANDS
  case class Invoice(recipient: String, date: Date, amount: Int)
  case class InvoiceBulk(invoices : List[Invoice])
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
      case InvoiceBulk(invoices) =>
        val invoiceIDs = latestInvoiceID to invoices.length
        val events = invoices.zip(invoiceIDs).map {
          pair =>
            val id = pair._2
            val invoice = pair._1
            InvoiceRecorded(id, invoice.recipient, invoice.date, invoice.amount)
        }
        persistAll(events) {e => {
          latestInvoiceID += 1
          totalAmount += e.amount
          log.info(s"persisted ${e.id}")
        }}

    }

    //handler called on recovery
    override def receiveRecover: Receive = {
      case InvoiceRecorded(id, _, _, amount) => {
        latestInvoiceID = id
        totalAmount += amount
        log.info(s"recovered invoice $latestInvoiceID for amount $totalAmount")
      }
    }

    // actor is stopped
    // supervisor pattern to rescue
    override def onPersistFailure(cause: Throwable, event: Any, seqNr: Long): Unit = {
      log.error(s"Fail to persist $event because of $cause")
      super.onPersistFailure(cause, event, seqNr)
    }

    //called, if journal throws exception when persisting the event
    override def onPersistRejected(cause: Throwable, event: Any, seqNr: Long): Unit = {
      log.error(s"Reject to persist $event because of $cause")
      super.onPersistRejected(cause, event, seqNr)
    }
  }

  val system = ActorSystem("persistent-actors")
  val accountant = system.actorOf(Props[Accountant], "simpleAccountant")

//  for (i <- 1 to 10) {
//    accountant ! Invoice("The safe company", new Date, 1000)
//  }

//  val invoices = for (i <- 1 to 5) yield Invoice("The safe company", new Date, 1000)
//  accountant ! InvoiceBulk(invoices.toList)

  /*
  never ever call persist and persistall in futures, persistactors handle that

   */
}
