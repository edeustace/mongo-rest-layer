import controllers.MongoRestLayerController
import net.sf.ehcache.search.aggregator.Count
import play.api.mvc.{Handler, RequestHeader}
import util.matching.Regex
import db.{ConnectionInitializer, DBConnect}

/**
 * A simple lookup abstraction
 */
trait HandlerLookup {
  def getList(collection: String): Option[Handler]

  def query(collection: String): Option[Handler]

  def getOne(collection: String, id: String): Option[Handler]

  def createOne(collection: String): Option[Handler]

  def updateOne(collection: String, id: String): Option[Handler]

  def deleteOne(collection: String, id: String): Option[Handler]

  def deleteAll(collection: String): Option[Handler]

  def count(collection: String): Option[Handler]
}

object DefaultLookup extends HandlerLookup {
  def getList(collection: String) = Some(MongoRestLayerController.list(collection))

  def query(collection: String): Option[Handler] = Some(MongoRestLayerController.query(collection))

  def getOne(collection: String, id: String): Option[Handler] = Some(MongoRestLayerController.getOne(collection, id))

  def createOne(collection: String): Option[Handler] = Some(MongoRestLayerController.createOne(collection))

  def updateOne(collection: String, id: String): Option[Handler] = Some(MongoRestLayerController.updateOne(collection, id))

  def deleteOne(collection: String, id: String): Option[Handler] = Some(MongoRestLayerController.deleteOne(collection, id))

  def deleteAll(collection: String): Option[Handler] = Some(MongoRestLayerController.deleteAll(collection))

  def count(collection: String) = Some(MongoRestLayerController.count(collection))
}


class MongoRestLayer(val rootPath: String,
                     val mongoUri: String,
                     routeLookup: HandlerLookup = DefaultLookup,
                     initializer : ConnectionInitializer = DBConnect) {

  require(rootPath != null && rootPath.startsWith("/"), "the path must start with a leading /")
  require(mongoUri != null && mongoUri.startsWith("mongodb://"), "the mongo url must start with mongodb://")

  initializer.init(mongoUri)

  val ListPath: Regex = (rootPath + """/([\w\d]*$)""").r
  val SinglePath: Regex = (rootPath + """/([\w\d]*?)/([\w\d]*$)""").r
  val CountPath: Regex = (rootPath + """/([\w\d]*?)/count""").r

  def handlerFor(request: RequestHeader): Option[Handler] = {

    def listOrOne(path: String, listFn: (String) => Option[Handler], oneFn: (String, String) => Option[Handler]): Option[Handler] = path match {
      case ListPath(collection) => listFn(collection)
      case SinglePath(collection, id) => oneFn(collection, id)
      case _ => None
    }

    request.method match {
      case "GET" => listOrOne(request.path, routeLookup.getList, routeLookup.getOne)
      case "DELETE" => listOrOne(request.path, routeLookup.deleteAll, routeLookup.deleteOne)
      case "POST" =>
        request.path match {
          case CountPath(collection) => routeLookup.count(collection)
          case _ => listOrOne(request.path, routeLookup.query, routeLookup.updateOne)
        }
      case "PUT" => request.path match {
        case ListPath(collection) => routeLookup.createOne(collection)
        case _ => None
      }
      case _ => None
    }
  }
}
