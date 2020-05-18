package part2eventsourcing

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import java.util.Date

import akka.persistence.PersistentActor

object MultiplePersisting extends App {
  //With every invoice
  //Acccount will persist
  // Tax
  // Invoice Record

  case class Invoice(recipient: String, date: Date, amount: Int)

  case class TaxRecord(id: String, recordID: Int, date: Date, totalAmount: Int)
  case class InvoiceRecord(invoiceRecordId: Int, invoice: Invoice)

  class DiligentAccountant(taxId: String, taxAuthority: ActorRef) extends PersistentActor with ActorLogging {

    var latestTaxRecordID = 0
    var latestInvoiceRecordID = 0

    override def persistenceId: String = "diligent-accountant"

    //persistence is also based on message passing
    //calls to persist is asynchronous
    //the below receivecommand  persists TaxRecord first and then InvoiceRecord
    // and also record and invoiceRecord sent and received to TaxAuthority in order
    //journals are implemented using actors

    /* for nested it ensures order
    [INFO] [akka://MultiplePersistsDemo/user/$a] TaxRecord(uk-123123123,0,Mon May 18 09:27:26 ICT 2020,333)
    [INFO] [akka://MultiplePersistsDemo/user/$a] InvoiceRecord(0,Invoice(Company,Mon May 18 09:27:26 ICT 2020,1000))
    [INFO] [akka://MultiplePersistsDemo/user/$a] 've received tax authority
    [INFO] [  akka://MultiplePersistsDemo/user/$a] 've received invoice authority
     */
    override def receiveCommand: Receive = {
      case invoice: Invoice =>
        persist(TaxRecord(taxId, latestTaxRecordID, invoice.date, invoice.amount/3)){
          record => {
            taxAuthority ! record //first message
            latestTaxRecordID += 1
            persist("'ve received tax authority") {
              event => {
                taxAuthority ! event //third message
              }
            }
          }
        }
        persist(InvoiceRecord(latestInvoiceRecordID, invoice)){
          invoiceRecord => {
            taxAuthority ! invoiceRecord //second message
            latestInvoiceRecordID += 1
            persist("'ve received invoice authority") {
              event => {
                taxAuthority ! event //fourth message
              }
            }
          }
        }
    }

    override def receiveRecover: Receive = {
      case event => log.info(s"recoverevent: ${event.toString}")
    }
  }
  
  object DiligentAccountant {
    def props(taxId: String, taxAuthority: ActorRef) =
      Props(new DiligentAccountant(taxId, taxAuthority))
  }

  class TaxAuthority extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  val system = ActorSystem("MultiplePersistsDemo")
  val taxAuthority = system.actorOf(Props[TaxAuthority])
  val accountant = system.actorOf(DiligentAccountant.props("uk-123123123", taxAuthority))

  accountant ! Invoice("Company", new Date, 1000)
  accountant ! Invoice("SuperCar", new Date, 2000)
}
