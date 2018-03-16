package actors

import java.util.Date

import actors.TaskActor.{Create, GetAll, GetById, TaskNotFound}
import akka.actor.ActorSystem
import akka.pattern.ask
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import dao.TaskDao
import model.Task
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import org.specs2.mock.Mockito

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class TaskActorTest extends TestKit(ActorSystem("TaskActorTest")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll with Mockito {

  override def beforeAll(): Unit = {}

  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  implicit val timeout: Timeout = 5.seconds

  "A TaskActor" should {

    "return list of all tasks for GetAll message" in {
      val taskDao = mock[TaskDao]
      val task = Task(Some(1), "name123", "", "", new Date(), new Date())

      taskDao.findAll returns Future.successful(Seq[Task](task))

      val taskActor = system.actorOf(TaskActor.props(taskDao))

      Await.result(taskActor ? GetAll, timeout.duration).asInstanceOf[Seq[Task]] shouldBe Seq[Task](task)
    }

    "return task for GetById message" in {
      val taskDao = mock[TaskDao]
      val task = Task(Some(1), "name123", "", "", new Date(), new Date())

      taskDao.findById(anyLong) returns Future.successful(task)

      val taskActor = system.actorOf(TaskActor.props(taskDao))

      Await.result(taskActor ? GetById(1L), timeout.duration).asInstanceOf[Task] shouldBe task
    }

    "return TaskNotFound for GetById message" in {
      val taskDao = mock[TaskDao]

      taskDao.findById(anyLong) returns Future.failed(new NoSuchElementException(""))

      val taskActor = system.actorOf(TaskActor.props(taskDao))

      Await.result(taskActor ? GetById(1L), timeout.duration).asInstanceOf[TaskNotFound] shouldBe TaskNotFound()
    }

    "create task for Create message" in {
      val taskDao = mock[TaskDao]

      taskDao.create(any[Task]) returns Future.successful(1L)

      val taskActor = system.actorOf(TaskActor.props(taskDao))

      Await.result(taskActor ? Create("name123", "", "", new Date(), new Date()), timeout.duration).asInstanceOf[Long] shouldBe 1L
    }

  }
}
