import play.api.{Play, GlobalSettings}
import play.api.mvc.{Handler, RequestHeader, Request}

object Global extends GlobalSettings {


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
