package model.definition

import java.sql.Timestamp
import java.util.Date

import model.Task
import slick.jdbc.H2Profile.api._


class TaskTable(tag: Tag) extends Table[Task](tag, "tasks"){
  implicit def mapDate = MappedColumnType.base[Date, Timestamp](
    d => new Timestamp(d.getTime),
    identity
  )

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name")
  def description = column[String]("description")
  def category = column[String]("category")
  def dueDate = column[Date]("dueDate")
  def createDate = column[Date]("createDate")
  def * = (id.?, name, description, category, dueDate, createDate) <> ((Task.apply _).tupled, Task.unapply)
}
