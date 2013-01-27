package jobs

import akka.util.duration._
import play.api.libs.concurrent.Akka
import play.api._

object Scheduler {
  
  def start(app: Application) {
    Akka.system(app).scheduler.schedule(0 seconds, 2 minutes) {
      println("!!! SCHEDULER is WORKING !!!")
    }
  }
  
  def shutdown(app: Application){
    println("!!! SCHEDULER has STOPPED !!!")
    Akka.system(app).shutdown()
  }
  
}