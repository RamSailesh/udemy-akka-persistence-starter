package part4practices

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.PersistentActor
import akka.persistence.journal.{EventSeq, ReadEventAdapter}
import com.typesafe.config.ConfigFactory

import scala.collection.mutable

object EventAdapters extends App {
  //structure
  case class Guitar(id: String, model: String, make: String, guitarType:String = "ACOUSTIC")
  //command
  case class AddGuitar(quantity: Int, guitar: Guitar)
  //event
  case class GuitarAdded(id: String, model: String, make: String, quantity: Int)
  case class GuitarAddedV2(id: String, model: String, make: String, quantity: Int, guitarType:String)
  //TODO :> Convet everything to V3
  case class GuitarAddedV3(id: String, model: String, make: String, quantity: Int, guitarType:String, somedata: String)

  //WriteEventAdapter - for backward compatiblity
  //actor -> write event adapter -> serializer -> journal

  class GuitarReadEventAdapter extends ReadEventAdapter {
    // journal -> serializer -> event adapter -> transform
    def fromJournal(event: Any, manifest: String): EventSeq = event match {
      case GuitarAdded(id, model, make, quantity) =>
        EventSeq.single(GuitarAddedV2(id, model, make, quantity, "ACOUSTIC"))
      case other => EventSeq.single(other)
    }
  }

  class InventoryManager extends PersistentActor with ActorLogging {
    val inventory = new mutable.HashMap[Guitar, Int]()

    override def persistenceId: String = "inventory-guitar-manager"

    override def receiveCommand: Receive = {
      case AddGuitar(quantity, guitar @ Guitar(id, model, make, guitarType)) => {
        persist(GuitarAddedV2(id, model, make, quantity, guitarType)) {
          e => {
            addGuitar(guitar, quantity)
            println(s"Added $quantity X $guitar to inventory")
          }
        }
      }
      case "print" =>
        println(s"current inventory is $inventory")
    }

    override def receiveRecover: Receive = {
//      case GuitarAdded(id, model, make, quantity) => {
//        val guitar = Guitar(id, model, make)
//        addGuitar(guitar, quantity)
//        println(s"Recovered $quantity X $guitar to inventory")
//      }
      case GuitarAddedV2(id, model, make, quantity,guitarType) => {
        val guitar = Guitar(id, model, make, guitarType)
        addGuitar(guitar, quantity)
        println(s"Recovered $quantity X $guitar to inventory")
      }
    }

    def addGuitar(guitar: Guitar, quantity:Int) = {
      val existingQuantity = inventory.getOrElse(guitar, 0)
      inventory.put(guitar, existingQuantity + quantity)
    }
  }

  val system = ActorSystem("eventAdapters", ConfigFactory.load().getConfig("eventAdapters"))
  val inventoryManager = system.actorOf(Props[InventoryManager])

//  val guitars = for (i <- 1 to 10) yield Guitar(s"$i", s"Hacker-$i", s"Rtjvm-$i")
 // guitars.foreach(g =>  inventoryManager ! AddGuitar(10, g))


  inventoryManager ! "print"
}
