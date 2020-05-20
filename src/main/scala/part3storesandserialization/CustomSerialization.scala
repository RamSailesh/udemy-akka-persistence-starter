package part3storesandserialization

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.PersistentActor
import akka.serialization.Serializer
import com.typesafe.config.ConfigFactory

//Commands
case class Register(user: String, email:String)

//Events
case class Registered(id: Int, user: String, email: String)

//Serializer
class UserRegistrationSerializer extends Serializer {
  val seperator = "||"

  override def identifier: Int = 1 //

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case event @ Registered(id, user, email)  =>
      println(s"serializing $event ")
      s"$id$seperator$user$seperator$email".getBytes()
    case _ => throw new IllegalArgumentException("Only Registered is serializable")
  }

  override def fromBinary(bytes: Array[Byte], manifest: Option[Class[_]]): AnyRef = {
    val string = new String(bytes)
    println(s"deserialized  $string")
    val values = string.split(seperator)
    Registered(1, values(1), values(2))
  }

  override def includeManifest: Boolean = false
}

class UserRegistrationActor extends PersistentActor with ActorLogging {

  var latestID = 0
  override def persistenceId: String = "registration-actor"

  override def receiveCommand: Receive = {
    case Register(user, email) =>
      persist(Registered(latestID, user, email)) {
        e => {
          latestID += 1
          log.info(s"Persisted $e")
        }
      }
  }

  override def receiveRecover: Receive = {
    case event @ Registered(id, _, _) => {
      log.info(s"recovered : $event")
      latestID = id
    }
  }
}

object CustomSerialization extends App {

  val system = ActorSystem("customserialization", ConfigFactory.load().getConfig("customSerializerDemo"))
  val userRegistrationActor  = system.actorOf(Props[UserRegistrationActor])

  userRegistrationActor ! Register("r", "r@agmail.com")
  userRegistrationActor ! Register("s", "s@agmail.com")
}
