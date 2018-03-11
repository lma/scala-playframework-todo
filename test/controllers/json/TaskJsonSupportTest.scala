package controllers.json

import java.text.SimpleDateFormat

import model.Task
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsError, Json}

class TaskJsonSupportTest extends PlaySpec with TaskJsonSupport {

  val sdf = new SimpleDateFormat(dateFormat)

  "TaskJsonSupport" should {

    "deserialize Task from JSON" in  {
      val json = Json.parse(
        """
          |{
          | "name": "ay",
          |	"description": "desc",
          |	"category": "cwsds",
          |	"dueDate": "2018-03-19T11:21:01",
          |	"createDate": "2018-03-08T11:01:26"
          |}
        """.stripMargin
      )

      val task = json.validate[Task].get

      task.name mustBe "ay"
      task.description mustBe "desc"
      task.category mustBe "cwsds"
      task.dueDate mustBe sdf.parse("2018-03-19T11:21:01")
      task.createDate mustBe sdf.parse("2018-03-08T11:01:26")
    }

    "return validation error because missing name" in {
      val json = Json.parse(
        """
          |{
          | "name": "",
          |	"description": "desc",
          |	"category": "cwsds",
          |	"dueDate": "2018-03-19T11:21:01",
          |	"createDate": "2018-03-08T11:01:26"
          |}
        """.stripMargin
      )

      val task = json.validate[Task]

      task.isError mustBe true
      (task.asInstanceOf[JsError].errors.toList)(0)._1.toString() mustBe "/name"
    }

    "serialize Task to JSON" in {
      val dueDate = sdf.parse("2018-03-19T11:21:01")
      val createDate = sdf.parse("2018-03-08T11:01:26")
      val task = Task(Some(1), "name1", "desc1", "category1", dueDate, createDate)

      val json = Json.toJson(task)

      (json \ "id").as[Long] mustBe 1
      (json \ "name").as[String] mustBe "name1"
      (json \ "description").as[String] mustBe "desc1"
      (json \ "category").as[String] mustBe "category1"
      (json \ "dueDate").as[Long] mustBe dueDate.getTime
      (json \ "createDate").as[Long] mustBe createDate.getTime
    }
  }
}
