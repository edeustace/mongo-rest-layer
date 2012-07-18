package brokers

import db.CollectionAction
import play.api.mvc._
import util.matching.Regex
import java.net.URLDecoder
import scala.Some
import org.jboss.netty.handler.codec.http.HttpRequest
import play.api.mvc.Request

/**
 * A Rest implementdation similar to the one provided by mongolab.com
 * @param layer
 */
class MongoLabRequestBroker(val layer: CollectionAction) extends RequestBroker {


  val Json = ("Content-Type" -> "application/json; charset=utf-8")

  val QueryPath: Regex = """/.*?/collections/([\w\d]*?)\?(.*)""".r
  val ListPath: Regex = """/.*?/collections/([\w|\d|_|-]*)\?*.*""".r
  val SinglePath: Regex = """/.*?/collections/([\w|\d|_|-]*?)/([\w|\d|-|_]*)\?*.*""".r
  val RootPath: Regex = "(.*)".r


  private def wrapSomeAction(result: String): Option[Action[AnyContent]] = {
    Some(
      Action(
        Results.Status(200).apply(result).withHeaders(Json)
      )
    )
  }

  /**
   * @deprecated
   *            Originally this would be the easier solution.
   *            But need to find out how to convert PlayDefaultUpstreamHandler -> Requests
   *            Or how to access the request body.
   * @param request the request to handle
   * @return either the string result or None if it can't handle the request
   */
  override def handle(request: RequestHeader): Option[String] = {

    request.method match {
      case "GET" => {
        request.uri match {
          case QueryPath(collection, paramString) => {
            val decoded: String = URLDecoder.decode(paramString, "UTF-8")
            val params: List[String] = decoded.split("&").toList
            val q = params.find(p => p.startsWith("q")).getOrElse("q= ").split("q=")(1)

            if (paramString.contains("c=true")) {
              Some(layer.count(collection, q))
            }
            else {
              val f = params.find(p => p.startsWith("f")).getOrElse("f= ").split("f=")(1)
              val l = params.find(p => p.startsWith("l")).getOrElse("l=20").split("l=")(1)
              val sk = params.find(_.startsWith("sk")).getOrElse("sk=0").split("sk=")(1)

              Some(layer.query(collection, q, f, l.toInt, sk.toInt))
            }
          }
          case SinglePath(collection, id) => Some(layer.getOne(collection, id))
          case ListPath(collection) => Some(layer.list(collection))
          case _ => None
        }

      }
      case "POST" => {
        request.uri match {
          case ListPath(collection) => {
            val fullRequest: Request[String] = request.asInstanceOf[Request[String]]
            Some(layer.createOne(collection, fullRequest.body))
          }
          case _ => None
        }
      }
      case "PUT" => {
        request.uri match {

          case SinglePath(collection, id) => {
            val data = request.asInstanceOf[Request[String]].body
            Some(layer.updateOne(collection,id,data))
          }

        }
      }
      case "DELETE" => {
        request.uri match {
          case SinglePath(collection, id) => Some(layer.deleteOne(collection, id))
          //Don't support DeleteAll
          case _ => None
        }
      }
      case _ => None
    }
  }

  /**
   * Don't know how to convert PlayDefaultUpstreamHandler requests or to parse their body.
   * So instead we return Action definitions, which allows us to parse normal Play Request Objects.
   * @param request
   * @return
   */
  override def handle2(request: RequestHeader): Option[Action[AnyContent]] = {

    request.method match {
      case "GET" => {
        request.uri match {
          case QueryPath(collection, paramString) => {
            val decoded: String = URLDecoder.decode(paramString, "UTF-8")
            val params: List[String] = decoded.split("&").toList
            val q = params.find(p => p.startsWith("q")).getOrElse("q= ").split("q=")(1)

            if (paramString.contains("c=true")) {
              wrapSomeAction(layer.count(collection, q))
            }
            else {
              val f = params.find(p => p.startsWith("f")).getOrElse("f= ").split("f=")(1)
              val l = params.find(p => p.startsWith("l")).getOrElse("l=20").split("l=")(1)
              val sk = params.find(_.startsWith("sk")).getOrElse("sk=0").split("sk=")(1)
              wrapSomeAction(layer.query(collection, q, f, l.toInt, sk.toInt))
            }
          }
          case SinglePath(collection, id) => wrapSomeAction(layer.getOne(collection, id))
          case ListPath(collection) => wrapSomeAction(layer.list(collection))
          case _ => None
        }

      }
      case "POST" => {
        request.uri match {
          case ListPath(collection) => {
            val a = Action {
              implicit request =>
                val data = request.body.asJson
                val string = data.get.toString()
                Results.Status(200).apply(layer.createOne(collection, string)).withHeaders(Json)
            }
            Some(a)
          }
          case _ => None
        }
      }
      case "PUT" => {
        request.uri match {

          case SinglePath(collection, id) => {
            val a = Action {
              implicit request =>
                val data = request.body.asJson
                val string = data.get.toString()
                Results.Status(200).apply(layer.updateOne(collection, id, string)).withHeaders(Json)
            }
            Some(a)
          }
          case _ => None
        }
      }
      case "DELETE" => {
        request.uri match {
          case SinglePath(collection, id) => wrapSomeAction(layer.deleteOne(collection, id))
          //Don't support DeleteAll
          case _ => None
        }
      }
      case _ => None
    }
  }

}

