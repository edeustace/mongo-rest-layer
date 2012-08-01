import brokers.{MongoLabRequestBroker, RequestBroker}
import play.api.mvc.{Results, Action, Handler, RequestHeader}
import util.matching.Regex
import db.{JsonCollectionAction, CollectionAction, ConnectionInitializer, DBConnect}

class MongoRestLayer(val rootPath: String,
                     val mongoUri: String,
                     val handler: RequestBroker = new MongoLabRequestBroker(JsonCollectionAction),
                     initializer: ConnectionInitializer = DBConnect) {

  require(rootPath != null && rootPath.startsWith("/"), "the path must start with a leading /")
  require(mongoUri != null && mongoUri.startsWith("mongodb://"), "the mongo url must start with mongodb://")

  initializer.init(mongoUri)

  val RootPath: Regex = (rootPath + "(.*)").r

  def handlerFor(request: RequestHeader): Option[Handler] = {

    request.path match {
      case RootPath(path) => {
        handler.handle2(request)
      }
      case _ => None
    }
  }
}
