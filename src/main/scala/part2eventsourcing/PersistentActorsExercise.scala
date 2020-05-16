package part2eventsourcing

import java.util.Date

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.{PersistentActor, Recovery}

import scala.collection.mutable.{HashSet, Map}

object PersistentActorsExercise extends App {


  case class Vote(citizenID: String, candidate: String)

  class VotingStation extends PersistentActor with ActorLogging {

    val citizens: HashSet[String] = HashSet[String]()
    val poll: Map[String, Int] = Map[String, Int]()

    override def persistenceId: String = "voting-station"

    override def receiveRecover: Receive = {
      case vote: Vote => {
        log.info(s"$vote")
        processVote(vote)
      }
    }


    def processVote(vote: Vote) = {
      poll(vote.candidate) = poll.getOrElse(vote.candidate, 0) + 1
    }

    override def receiveCommand: Receive = {
      case vote: Vote =>
        if (!citizens.contains(vote.citizenID)) {
          persist(vote) {
            e => {
              processVote(vote)
              log.info(s"vote")
            }
          }
        }
      case "print" => log.info(s"${poll.filter(_._2 == poll.values.max)}")

    }
  }

  val system = ActorSystem("PersistentActorsExercise")
  val votingStation = system.actorOf(Props[VotingStation])

//  votingStation ! Vote("1022", "ram")
//  votingStation ! Vote("1010", "ram")
//  votingStation ! Vote("417", "ram")
//  votingStation ! Vote("417", "ram")
//  votingStation ! Vote("930", "wm")
//
//    votingStation ! "print"
//  votingStation ! Vote("226", "wm")
//  votingStation ! Vote("228", "wm")
//  votingStation ! Vote("308", "wm")


  votingStation ! "print"
}
