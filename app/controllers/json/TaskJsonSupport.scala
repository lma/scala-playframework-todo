package controllers.json

import java.text.SimpleDateFormat
import java.util.Date

import model.Task
import play.api.libs.json._
import play.api.libs.json.Reads.minLength
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._

trait TaskJsonSupport {

  implicit val taskWrites: OWrites[Task] = Json.writes[Task]

  val dateFormat = "yyyy-MM-dd'T'HH:mm:ss"

  val dateRead: Reads[Date] = Reads[Date](js =>
    js.validate[String].map[Date](dtString => {
      new SimpleDateFormat(dateFormat).parse(dtString)
    })
  )

  def checkDate: Reads[String] = Reads.filterNot(JsonValidationError("past date"))(aa => {
    val date = new SimpleDateFormat(dateFormat).parse(aa)
    date.before(new Date())
  })

  implicit val taskReads: Reads[Task] = (
    (JsPath \ "id").readNullable[Long] and
      (JsPath \ "name").read[String] (minLength[String](2)) and
      (JsPath \ "description").read[String] and
      (JsPath \ "category").read[String] (minLength[String](2)) and
      (JsPath \ "dueDate").read[Date] (dateRead keepAnd checkDate) and
      (JsPath \ "createDate").read[Date] (dateRead)
    ) (Task.apply _)
}
