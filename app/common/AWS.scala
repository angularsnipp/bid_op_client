package common

import fly.play.s3._

object AWS {

  def bucket(url: String): String = {
    try {
      S3("homeland-assets").url(url, 10)
    } catch {
      case e: Throwable => ""
    }
  }

}