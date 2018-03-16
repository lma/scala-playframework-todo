package actors

import java.util.Date

import akka.actor._
import akka.pattern.pipe
import dao.TaskDao
import model.Task

import scala.util.{Failure, Success}

object TaskActor {

  def props(dao: TaskDao): Props = Props(new TaskActor(dao))

  case class Create(name: String, description: String, category: String, dueDate: Date, createDate: Date)

  case class Update(id: Long, name: String, description: String, category: String, dueDate: Date, createDate: Date)

  case class GetAll()

  case class Delete(id: Long)

  case class GetById(id: Long)

  case class CheckDueDate()

  case class TaskNotFound()

}

class TaskActor(dao: TaskDao) extends Actor with ActorLogging {

  import TaskActor._
  import dao._

  implicit val ec = context.dispatcher

  override def receive: Receive = {
    case GetAll                                                       => findAll pipeTo sender()
    case GetById(id)                                                  => getAndCheckTask(id)
    case Delete(id)                                                   => delete(id)   pipeTo sender()
    case CheckDueDate                                                 => findAllWithDueDateExceeded pipeTo sender()
    case Create(name, description, category, dueDate, createDate)     => create(Task(None, name, description, category, dueDate, createDate)) pipeTo sender()
    case Update(id, name, description, category, dueDate, createDate) => checkAndUpdateTask(id, name, description, category, dueDate, createDate)
  }

  private def getAndCheckTask(id: Long) = {
    val originSender = sender()

    findById(id).onComplete {
      case Success(task) => originSender ! task
      case Failure(_) => originSender ! TaskNotFound()
    }
  }

  private def checkAndUpdateTask(id: Long, name: String, description: String, category: String, dueDate: Date, createDate: Date) = {
    val originSender = sender()

    findById(id).onComplete {
      case Success(_) => update(Task(None, name, description, category, dueDate, createDate), id) pipeTo originSender
      case Failure(_) => originSender ! TaskNotFound()
    }
  }
}
