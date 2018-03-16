import controllers.TaskController
import model.Task
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.libs.json.{JsString, Json, OWrites}
import play.api.test.Helpers._
import play.api.test.{FakeHeaders, FakeRequest}
import play.mvc.Http

/**
 * Functional tests start a Play application internally, available
 * as `app`.
 */
class FunctionalSpec extends PlaySpec with GuiceOneAppPerSuite with ScalaFutures {

  implicit val taskWrites: OWrites[Task] = Json.writes[Task]
  def homeController = app.injector.instanceOf(classOf[TaskController])

  "Routes" should {

    "send 404 on a bad request" in  {
      route(app, FakeRequest(GET, "/boum")).map(status(_)) mustBe Some(NOT_FOUND)
    }

    "send 200 on a good request" in  {
      route(app, FakeRequest(GET, "/tasks")).map(status(_)) mustBe Some(OK)
    }

  }

  "HomeController" should {

    "return empty list of tasks" in {
      val home = route(app, FakeRequest(GET, "/tasks")).get
      status(home) mustBe Status.OK
      contentType(home) mustBe Some("application/json")
      contentAsJson(home) mustBe Json.toJson(Seq[Task]())
    }
  }

  "HomeController" should {
    "return OK through route events" in {
      val request = FakeRequest(method = GET, path = "/events")
      route(app, request) match {
        case Some(future) =>
          whenReady(future) { result =>
            result.header.status mustEqual(OK)
          }
        case None =>
          fail
      }
    }
  }

  "HomeController" should {

    "return one saved ID" in {

      val json = Json.obj(
        "name" -> JsString("name"),
        "description" -> JsString("description"),
        "category" -> JsString("category"),
        "dueDate" -> JsString("2038-03-09T09:35:17"),
        "createDate" -> JsString("2038-03-09T09:35:17")
      )

      val value = FakeRequest(method = POST,
        uri = "/tasks",
        headers = FakeHeaders(
          Seq(
            "Content-type" -> "application/json",
            Http.HeaderNames.HOST -> "localhost"
          )
        ),
        body = json)

      val home = route(app, value).get

      status(home) mustBe Status.CREATED
      contentType(home) mustBe Some("application/json")
      contentAsJson(home) mustBe Json.toJson(1)
    }
  }

  "HomeController" should {

    "return error message - empty name" in {

      val json = Json.obj(
        "name" -> JsString(""),
        "description" -> JsString("description"),
        "category" -> JsString("category"),
        "dueDate" -> JsString("2038-03-09T09:35:17"),
        "createDate" -> JsString("2038-03-09T09:35:17")
      )

      val value = FakeRequest(method = POST,
        uri = "/tasks",
        headers = FakeHeaders(
          Seq(
            "Content-type" -> "application/json",
            Http.HeaderNames.HOST -> "localhost"
          )
        ),
        body = json)

      val home = route(app, value).get

      status(home) mustBe Status.BAD_REQUEST
      contentType(home) mustBe Some("application/json")
      contentAsString(home) must include ("obj.name")
      contentAsString(home) must include ("error.minLength")
    }
  }
}
