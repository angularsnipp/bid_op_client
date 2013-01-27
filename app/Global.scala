import play.api._
import play.api.Play._
import jobs._

object Global extends GlobalSettings {

  override def onStart(app: Application) {
    Logger.info("!!! Application has STARTED...")
    if (isDev(app)) Scheduler.start(app)

  }

  override def onStop(app: Application) {
    Logger.info("!!! Application has FINISHED...")
    Scheduler.shutdown(app)
  }

}
