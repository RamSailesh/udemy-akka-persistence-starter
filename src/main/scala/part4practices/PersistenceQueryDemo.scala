package part4practices

import akka.actor.ActorSystem
import akka.persistence.cassandra.journal.CassandraJournal
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query.PersistenceQuery
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory


object PersistenceQueryDemo extends App {
  val actorSystem = ActorSystem("PersistenceQueryDemo", ConfigFactory.load().getConfig("persistenceQuery"))
  val readJournal = PersistenceQuery(actorSystem).readJournalFor[CassandraReadJournal](CassandraReadJournal.Identifier)

  val persistenceIds = readJournal.persistenceIds()

  implicit val actorSystemM = (ActorMaterializer()(actorSystem))
  //real-time if new actor are added these are added at run-time
  persistenceIds.runForeach(x => {
    println(s"id: $x")
  })


  val events  = readJournal.eventsByPersistenceId("coupon-manager", 0, Long.MaxValue)
  events.runForeach(x => {
    println(s"events : $x")
  })(ActorMaterializer()(actorSystem))


}
