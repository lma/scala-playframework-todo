package controllers.json

import model._
import play.api.libs.json.Json

trait UserJsonSupport {

  implicit val geoReads = Json.reads[Geo]
  implicit val addressReads = Json.reads[Address]
  implicit val companyReads = Json.reads[Company]
  implicit val userReads = Json.reads[User]
}
