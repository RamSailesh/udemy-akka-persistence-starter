package part3storesandserialization

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory

object CassandraDemo extends App {
  val system = ActorSystem("cassandraDemoSystem", ConfigFactory.load().getConfig("cassandraDemo"))
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
