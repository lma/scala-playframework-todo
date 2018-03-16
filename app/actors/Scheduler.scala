package actors

import scala.concurrent.duration.FiniteDuration

trait Scheduler {
  def interval: FiniteDuration
}
