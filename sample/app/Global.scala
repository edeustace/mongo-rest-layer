import play.api.{Play, GlobalSettings}
import play.api.mvc.{Handler, RequestHeader, Request}

object Global extends GlobalSettings {


  val mongoUri : String = "mongodb://test:test@localhost:27017/content-tagger-dev"
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
