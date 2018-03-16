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
import play.api.libs.ws.WSClient
import play.api.mvc._
import play.api.{Configuration, Logger}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ExternalApiController @Inject()(cc: ControllerComponents,
                                      ws: WSClient,
                                      config: Configuration)(implicit exec: ExecutionContext)
                               extends AbstractController(cc) with UserJsonSupport {

  def extApi = Action.async {

    val restApi = config.get[String]("users.rest.api")
    ws.url(restApi).get()
      .map{
        response =>
          val users = (response.json).validate[Seq[User]].get
          Ok(s"Results from: ${restApi}\nusers size: ${users.size}\n" + (users.map(u => s"${u.id}. ${u.name}").mkString("\n")).toString)
      }
  }
}
