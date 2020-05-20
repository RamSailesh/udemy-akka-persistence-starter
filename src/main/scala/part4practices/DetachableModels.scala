package part4practices

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.PersistentActor
import akka.persistence.journal.{EventAdapter, EventSeq}
import com.typesafe.config.ConfigFactory
import part4practices.DomainModel.CouponApplied

import scala.collection.mutable

object DetachableModels extends App {
  import DomainModel._

  class CouponManager extends PersistentActor with ActorLogging {

    val coupons: mutable.Map[String, User] = new mutable.HashMap[String, User]

    override def persistenceId: String = "coupon-manager"

    override def receiveCommand: Receive = {
      case ApplyCoupon(coupon, user) =>
        if(!coupons.contains(coupon.code)) {
          persist(CouponApplied(coupon.code, user)) {
            e => {
              log.info(s"Coupon applied $coupon")
              coupons.put(coupon.code, user)
            }
          }
        }
    }

    override def receiveRecover: Receive = {
      case event @ CouponApplied(couponCode, user) =>
        log.info(s"recovered $event")
        coupons.put(couponCode, user)
    }
  }

  val system = ActorSystem("detaching-models1", ConfigFactory.load().getConfig("detachingModels"))
  val couponManager = system.actorOf(Props[CouponManager], "cm1")

//  for (i <- 1 to 5) {
//    val coupon = Coupon(s"mega coupon $i", 100)
//    val user = User(s"r$i", s"r$i@rrr.com")
//    couponManager ! ApplyCoupon(coupon, user)
//  }
 }

object DomainModel {
  case class User(id: String, email: String, name: String)
  case class Coupon(code: String, promotionAmount: Int)

  case class ApplyCoupon(coupon: Coupon, user: User)
  case class CouponApplied(code: String, user: User)
}

object DataModel {
  case class WrittenCouponApplied(code: String, userID: String, userEmail: String)
  case class WrittenCouponAppliedV2(code: String, userID: String, userEmail: String, userName: String)
}
}


class ModelAdapter extends EventAdapter {
  import DataModel._
  import DomainModel._

  override def manifest(event: Any): String = "cma"

  //journal(db/cassandra) -> serializer -> fromJournal -> actor
  override def fromJournal(event: Any, manifest: String): EventSeq = event match {
    case couponApplied @ WrittenCouponApplied(code, userId, userEmail) =>
      println(s"fromJournal $couponApplied")
      EventSeq.single(CouponApplied(code, User(userId, userEmail, "")))
    case couponApplied @ WrittenCouponAppliedV2(code, userId, userEmail, userName) =>
      println(s"fromJournal $couponApplied")
      EventSeq.single(CouponApplied(code, User(userId, userEmail, userName)))
    case other =>  {
      println(s"fromJournal Other $other")
      EventSeq.single(other)
    }
  }

  //actor -> serializer -> toJournal -> journal
  override def toJournal(event: Any): Any = event match {
    case event @ CouponApplied(code, user) =>
      println(s"toJournal $event")
      EventSeq.single(WrittenCouponAppliedV2(code, user.id, user.email, user.name))
    case other =>  {
      println(s"toJournalOther $other")
      EventSeq.single(other)
    }
  }
}
