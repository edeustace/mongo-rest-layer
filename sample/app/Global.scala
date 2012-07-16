import play.api.{Play, GlobalSettings}
import play.api.mvc.{Handler, RequestHeader, Request}

object Global extends GlobalSettings {


  val mongoLab = "mongodb://corespring:iFt4inav@ds033307.mongolab.com:33307/metadata"
  val mongoUri = "mongodb://test:test@localhost:27017/content-tagger-dev"
  val mongoRestLayer = new MongoRestLayer("/mongo-rest", mongoUri)

  override def onRouteRequest(request:RequestHeader) : Option[Handler] = {

    val handler : Option[Handler] = mongoRestLayer.handlerFor(request)

    handler match {
      case Some(h) => handler
      case _ => Play.maybeApplication.flatMap(_.routes.flatMap { router =>
        router.handlerFor(request)
      })
    }
  }
}
