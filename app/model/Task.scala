package model

import java.util.Date

case class Task(id: Option[Long], name: String, description: String, category: String,
                dueDate: Date, createDate: Date)
