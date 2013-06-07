import sbt._
import Keys._
import play.Project._

object Dependencies {
  val scalatest = "org.scalatest" %% "scalatest" % "1.9" % "test"
  //val codahale = "com.codahale" %% "jerkson" % "0.5.0"
  val fasterxml = "com.fasterxml.jackson.core" % "jackson-databind" % "2.0.0-RC3"
  val squeryl_orm = "org.squeryl" %% "squeryl" % "0.9.5-6"
  val postgresDriver = "postgresql" % "postgresql" % "9.1-901-1.jdbc4"
  val yandex_metrika = "com.github.krispo" % "yandex-metrika_2.10" % "0.1-SNAPSHOT"
  val yandex_direct = "com.github.krispo" % "yandex-direct_2.10" % "0.1-SNAPSHOT"
  val aws_S3 = "nl.rhinofly" %% "api-s3" % "2.6.1"
}

object Resolvers {
  val sonatype = "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
  val rhinofly = "Rhinofly Internal Repository" at "http://maven-repository.rhinofly.net:8081/artifactory/libs-release-local"
  //val codahale = "codahale" at "http://repo.codahale.com"
}

object ApplicationBuild extends Build {

  val appName = "bid_op_client"
  val appVersion = "1.0-SNAPSHOT"

  import Dependencies._
  import Resolvers._
  val appDependencies = Seq(
    // Add your project dependencies here
    jdbc, anorm, scalatest, fasterxml, squeryl_orm, postgresDriver, yandex_metrika, yandex_direct, aws_S3) //, codahale)

  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here
    testOptions in Test := Nil,
    resolvers ++= Seq(sonatype, rhinofly))
  //resolvers += Resolvers.codahale)/**/

}
