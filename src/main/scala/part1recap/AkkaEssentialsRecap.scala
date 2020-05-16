package part1recap

object AkkaEssentialsRecap extends App {
  /*
  -> ActorSystem and how actors are created, scheduled for processing messages
  -> Create Actor
      -> Can get reference of Actor,
      -> receive (Partial Function from Any to Unit)
      -> Actor Encapsulation
      -> How to communicate with actor? (tell, ask)
      -> Asynchronous Messages
      -> Each message is executed atomically
      -> No locking required
      -> context.become -> change the message handler from receive
      -> Stash - store the messages (stash(), unstashall())
  -> actor lifecycle
  -> stop actors
  -> MailBoxes
  -> Logging
  -> Supervision (How parent can respond to child actors behavior)
  -> Dispatchers
  -> Routers
  -> Mailboxes
  -> Schedulers
  -> FiniteStateMachines
  -> Ask Pattern (returns a Future)
  -> Pipe Pattern (communicate result of a future to Another actor)
   */
}
