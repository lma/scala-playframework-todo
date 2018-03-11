package controllers

import javax.inject._

import actors.TaskActor
import actors.TaskActor.TaskNotFound
import akka.actor.ActorRef
import akka.pattern.ask
import akka.stream.scaladsl.Source
import akka.util.Timeout
import akka.{Done, NotUsed}
import controllers.json.{TaskJsonSupport, UserJsonSupport}
import model._
import play.api.cache._
import play.api.http.ContentTypes
import play.api.libs.EventSource
import play.api.libs.json._
import play.api.libs.ws.WSClient
import play.api.mvc._
import play.api.{Configuration, Logger}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

@Singleton
class HomeController @Inject()(cc: ControllerComponents,
                               @Named("task-actor") taskActor: ActorRef,
                               cache: AsyncCacheApi,
                               ws: WSClient,
                               config: Configuration)(implicit exec: ExecutionContext)
  extends AbstractController(cc) with TaskJsonSupport with UserJsonSupport {

  implicit val timeout: Timeout = 5.seconds

  def getAll: Action[AnyContent] = Action.async {
    (taskActor ? TaskActor.GetAll).mapTo[Seq[Task]].map { resp =>
      Ok(Json.toJson(resp))
    }
  }

  def getById(id: Long): Action[AnyContent] = Action.async {
    implicit val taskWrites: OWrites[Task] = Json.writes[Task]
    (taskActor ? TaskActor.GetById(id)).map {
      case TaskNotFound() => BadRequest(Json.toJson(s"Task ${id} not found"))
      case t: Task => Ok(Json.toJson(t))
    }
  }

  def delete(id: Long): Action[AnyContent] = Action.async {
    (taskActor ? TaskActor.Delete(id)).mapTo[Int].map { m =>
      Ok(Json.toJson(m))
    }
  }

  def create: Action[JsValue] = Action.async(parse.json) { request =>
    val taskResult = request.body.validate[Task]
    taskResult.fold(
      errors => {
        Future.successful(BadRequest(Json.obj("status" -> "OK", "message" -> JsError.toJson(errors))))
      },
      task => {
        addCategoryToCache(task)
          .flatMap(_ => taskActor ? TaskActor.Create(task.name, task.description, task.category, task.dueDate, task.createDate))
          .mapTo[Long]
          .map{ m => Ok(Json.toJson(m))}
          .recover({
            //TODO error handling
            case _ => {
              InternalServerError(Json.toJson("Something went wrong!"))
            }
         })
      }
    )
  }

  def update(id: Long): Action[JsValue] = Action.async(parse.json) { request =>
    val taskResult = request.body.validate[Task]
    taskResult.fold(
      errors => {
        Future {
          BadRequest(Json.obj("status" -> "OK", "message" -> JsError.toJson(errors)))
        }
      },
      task => {
        addCategoryToCache(task)
          .flatMap(_ => taskActor ? TaskActor.Update(id, task.name, task.description, task.category, task.dueDate, task.createDate))
          .map {
            case TaskNotFound() => BadRequest(Json.toJson(s"Task ${id} not found"))
            case m => Ok(Json.toJson(m.asInstanceOf[Int]))
          }.recover({
            case e => {
              Logger.error("Exception with task update.", e)
              InternalServerError(Json.toJson("Something went wrong!"))
            }
        })
      }
    )
  }

  def events = Action {
    val source = Source
      .tick(0.seconds, 10.seconds, NotUsed)
      .map(_ => taskToMessage)

    Ok.chunked(source via EventSource.flow).as(ContentTypes.EVENT_STREAM)
  }

  def categories: Action[AnyContent] = Action.async {
    import scala.collection.mutable.Set
    cache.get[Set[String]]("category").map{
      case Some(x) => Ok(Json.toJson(x))
      case None => Ok(Json.toJson("no cached categories"))
    }
  }

  def extApi = Action.async {

    val restApi = config.get[String]("users.rest.api")
    ws.url(restApi).get()
      .map{
        response =>
          val users = (response.json).validate[Seq[User]].get
          Ok(s"Results from: ${restApi}\nusers size: ${users.size}\n" + (users.map(u => s"${u.id}. ${u.name}").mkString("\n")).toString)
      }
  }

  private def taskToMessage = {
    Await.result((taskActor ? TaskActor.CheckDueDate)
      .map(r => r.asInstanceOf[Seq[Task]].map(t => t.id.get).mkString("\n")), Duration.Inf)
  }

  private def addCategoryToCache(task: Task): Future[Done] = {
    import scala.collection.mutable.Set

    cache.get[Set[String]]("category").map {
      case Some(x) => x += task.category
      case None => Set(task.category)
    }.flatMap(f => cache.set("category", f))
  }
}
