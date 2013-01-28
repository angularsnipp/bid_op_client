import play.api._
import play.api.Play._
import jobs._

object Global extends GlobalSettings {

  override def onStart(app:Application) {
    Logger.info("!!! Application has STARTED...")
    if (isDev) Scheduler.start

  }

  override def onStop(app:Application) {
    Logger.info("!!! Application has FINISHED...")
    Scheduler.shutdown
  }

}
