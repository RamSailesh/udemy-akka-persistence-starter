package part3storesandserialization

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.{PersistentActor, RecoveryCompleted, SaveSnapshotFailure, SaveSnapshotSuccess, SnapshotOffer}
import com.typesafe.config.ConfigFactory
import part3storesandserialization.SimplePersistentActor

object LocalStores extends App {

  val system = ActorSystem("local-stores", ConfigFactory.load().getConfig("localStores"))
  val persistentActor = system.actorOf(Props[SimplePersistentActor])
  for(i <- 1 to 10) {
    persistentActor ! s"I love akka $i"
  }

  persistentActor ! "print"
  persistentActor ! "snap"

  for(i <- 11 to 20) {
    persistentActor ! s"I love akka $i"
  }

}
