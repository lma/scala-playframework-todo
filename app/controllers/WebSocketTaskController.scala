package controllers

import javax.inject._

import actors.TaskActor.TaskNotFound
import actors.{DueDateWebSocketActor, TaskActor}
import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.Timeout
import akka.{Done, NotUsed}
import controllers.json.{TaskJsonSupport, UserJsonSupport}
import dao.TaskDao
import model._
import play.api.cache._
import play.api.db.slick.DatabaseConfigProvider
import play.api.http.ContentTypes
import play.api.libs.EventSource
import play.api.libs.json._
import play.api.libs.streams.ActorFlow
import play.api.libs.ws.WSClient
import play.api.mvc._
import play.api.{Configuration, Logger}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

@Singleton
class WebSocketTaskController @Inject()(system: ActorSystem,
                                        cc: ControllerComponents,
                                        dbConfigProvider: DatabaseConfigProvider,
                                        config: Configuration)(implicit exec: ExecutionContext, actSys: ActorSystem, mat: Materializer)
                               extends AbstractController(cc) with TaskJsonSupport with UserJsonSupport {

  implicit val timeout: Timeout = 5.seconds

  val taskActor: ActorRef = system.actorOf(TaskActor.props(new TaskDao(dbConfigProvider)))

  def events = Action {
    val source = Source
      .tick(0.seconds, 10.seconds, NotUsed)
      .map(_ => taskToMessage)

    Ok.chunked(source via EventSource.flow).as(ContentTypes.EVENT_STREAM)
  }

  def socket = WebSocket.accept[String, String] { request =>
    ActorFlow.actorRef { out =>
      DueDateWebSocketActor.props(out, taskActor)
    }
  }

  private def taskToMessage = {
    Await.result((taskActor ? TaskActor.CheckDueDate)
      .map(r => r.asInstanceOf[Seq[Task]].map(t => t.id.get).mkString("\n")), Duration.Inf)
  }
}
