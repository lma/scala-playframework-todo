package actors

import java.util.Date

import actors.TaskActor.CheckDueDate
import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import model.Task
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import org.specs2.mock.Mockito

import scala.concurrent.duration._


class DueDateWebSocketActorTest extends TestKit(ActorSystem("DueDateWebSocketActorTest")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll with Mockito {

  override def beforeAll(): Unit = {}

  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  "A DueDateWebSocketActor" should {

    "send CheckDueDate message" in {
      val controllerActor = TestProbe()
      val taskActor = TestProbe()

      system.actorOf(Props(new DueDateWebSocketActor(controllerActor.ref, taskActor.ref) {
        override def interval: FiniteDuration = 20.second
      }))

      taskActor.expectMsg(CheckDueDate)
      taskActor.reply(Seq(
        Task(Some(1), "", "", "", new Date(), new Date())
      ))
      controllerActor.expectMsg("I received tasks: 1")
    }
  }
}
