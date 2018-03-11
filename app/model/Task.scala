package model

import java.util.Date

case class Task(id: Option[Long], name: String, description: String, category: String,
                dueDate: Date, createDate: Date) {
  /*require(!name.isEmpty, "name must not be empty")
  require(!category.isEmpty, "category must not be empty")
  require(dueDate != null && dueDate.after(new Date()), "dueDate is null or is from past")
  require(createDate != null && createDate.after(new Date()), "createDate is null or is from past")*/
}
