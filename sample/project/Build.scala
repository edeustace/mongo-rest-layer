import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

  val appName = "mongo-rest-layer-sample"
  val appVersion = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    "mongo-rest-layer" %% "mongo-rest-layer" % "0.1-SNAPSHOT",
    "org.mongodb" %% "casbah" % "2.4.0"
  )

  val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
    // Add your own project settings here
    resolvers += "Local Play Repository" at "file:///Users/edeustace/dev/frameworks/play-2.0.2/repository/local"
  )

}
