package actors

import actors.DueDateWebSocketActor.AskDueDate
import actors.TaskActor.CheckDueDate
import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import model.Task

import scala.concurrent.duration._

object DueDateWebSocketActor {
  case class AskDueDate()

  def props(out: ActorRef, taskActor: ActorRef) = Props(new DueDateWebSocketActor(out, taskActor))
}

class DueDateWebSocketActor(out: ActorRef, taskActor: ActorRef) extends Actor with ActorLogging with Scheduler {

  implicit val ec = context.dispatcher
  implicit val timeout: Timeout = 5.seconds
  override def interval: FiniteDuration = 5.second

  private val taskScheduler: Cancellable = context.system.scheduler.schedule(0.microseconds, interval, self, AskDueDate)

  def receive = {
    case AskDueDate => {
      (taskActor ? CheckDueDate).mapTo[Seq[Task]]
        .map(tasks => {
          println("I received tasks: " + tasks.size)
          out ! ("I received tasks: " + tasks.size)
        })
        .recover({
          case e => {
            e.printStackTrace
            out ! "Error"
          }
        })
    }
    case msg: String =>
    out ! ("I received your message: " + msg)
  }

  override def postStop(): Unit = {
    taskScheduler.cancel()
  }

}
