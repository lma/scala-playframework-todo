package controllers

import javax.inject._

import actors.TaskActor
import actors.TaskActor.TaskNotFound
import akka.Done
import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import controllers.json.{TaskJsonSupport, UserJsonSupport}
import dao.TaskDao
import model._
import play.api.cache._
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json._
import play.api.mvc._
import play.api.{Configuration, Logger}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TaskController @Inject()(system: ActorSystem,
                               cc: ControllerComponents,
                               cache: AsyncCacheApi,
                               dbConfigProvider: DatabaseConfigProvider,
                               config: Configuration)(implicit exec: ExecutionContext)
                               extends AbstractController(cc) with TaskJsonSupport with UserJsonSupport {

  implicit val timeout: Timeout = 5.seconds

  val taskActor: ActorRef = system.actorOf(TaskActor.props(new TaskDao(dbConfigProvider)))

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
          .map{ m => Created(Json.toJson(m))}
          .recover({
            case e => {
              Logger.error("Something went wrong.", e)
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

  def categories: Action[AnyContent] = Action.async {
    import scala.collection.mutable.Set
    cache.get[Set[String]]("category").map{
      case Some(x) => Ok(Json.toJson(x))
      case None => Ok(Json.toJson("no cached categories"))
    }
  }

  private def addCategoryToCache(task: Task): Future[Done] = {
    import scala.collection.mutable.Set

    cache.get[Set[String]]("category").map {
      case Some(x) => x += task.category
      case None => Set(task.category)
    }.flatMap(f => cache.set("category", f))
  }
}
