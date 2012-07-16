package controllers

import play.api.mvc.{AnyContent, Request, Action, Controller}
import com.mongodb.casbah.MongoCollection
import com.mongodb.DBObject
import db.DBConnect
import com.codahale.jerkson.Json.generate
import org.bson.types.ObjectId

object MongoRestLayerController extends Controller {

  val Json = ("Content-Type" -> "application/json; charset=utf-8")

  case class Oid(oid: String)

  case class FailedResponse(success: Boolean = false)

  case class NoItemFound(id: String, success: Boolean = false, message: String = "no item found")

  case class DeleteItemResponse(success: Boolean, id: String)


  def list(collection: String) = Action {
    implicit request =>
      val dbCollection: MongoCollection = DBConnect.collection(collection)
      Ok(generate(dbCollection.map(processId))).withHeaders(Json)
  }

  def query(collection: String) = Action {
    implicit request =>

      getFormParameters("query", "fields") match {
        case List(Some(queryString), Some(fieldsString)) => {
          val dbCollection: MongoCollection = DBConnect.collection(collection)
          val query: DBObject = com.mongodb.util.JSON.parse(queryString).asInstanceOf[DBObject]
          val fields: DBObject = com.mongodb.util.JSON.parse(fieldsString).asInstanceOf[DBObject]
          val result = dbCollection.find(query, fields)
          Ok(generate(result.map(processId))).withHeaders(Json)
        }
        case _ => Ok(generate(FailedResponse())).withHeaders(Json)
      }
  }

  def count(collection: String) = Action {
    implicit request =>
      getFormParameters("query") match {
        case List(Some(queryString)) => {
          val dbCollection: MongoCollection = DBConnect.collection(collection)
          val query: DBObject = com.mongodb.util.JSON.parse(queryString).asInstanceOf[DBObject]
          Ok(generate(dbCollection.count(query))).withHeaders(Json)
        }
        case _ => Ok(generate(FailedResponse())).withHeaders(Json)
      }
  }

  def getOne(collection: String, id: String) = Action {
    implicit request =>
      val dbCollection: MongoCollection = DBConnect.collection(collection)
      val item: Option[DBObject] = dbCollection.findOneByID(new ObjectId(id))
      item match {
        case Some(itemData) => Ok(generate(processId(itemData))).withHeaders(Json)
        case _ => Ok(generate(NoItemFound(id))).withHeaders(Json)
      }
  }

  def createOne(collection: String) = Action {
    implicit request =>

      getFormParameters("item") match {
        case List(Some(itemString)) => {
          val dbCollection: MongoCollection = DBConnect.collection(collection)
          val itemData: DBObject = com.mongodb.util.JSON.parse(itemString).asInstanceOf[DBObject]
          val result = dbCollection.insert(itemData)
          val processed = processId(itemData)
          Ok(generate(processed)).withHeaders(Json)
        }
        case _ => Ok(generate(FailedResponse())).withHeaders(Json)
      }
  }

  /**
   * Updates a document - aka overwrites the document completely.
   * This is how mongolab.com REST api works too.
   * To change this you need to add modifiers to the request.
   * @see http://www.mongodb.org/display/DOCS/Updating
   * @example
   * curl http://localhost:9000/mrl/items/4ffff8c43004ad7a7c441528
   * --request POST
   * --data 'item={ "$set":{"blah5":"blah5"}}'
   * @param collection
   * @param id
   *
   * @return
   */
  def updateOne(collection: String, id: String) = Action {
    implicit request =>
      val dbCollection: MongoCollection = DBConnect.collection(collection)
      getFormParameters("item") match {
        case List(Some(updateItemString)) => {
          val updateObject: DBObject = com.mongodb.util.JSON.parse(updateItemString).asInstanceOf[DBObject]
          val itemToUpdate: Option[DBObject] = dbCollection.findOneByID(new ObjectId(id))

          itemToUpdate match {
            case Some(foundItemToUpdate) => {
              val item: Option[DBObject] = dbCollection.findAndModify(foundItemToUpdate, updateObject)
              getOne(collection,id)(request)
            }
            case _ => Ok(generate("couldn't find item")).withHeaders(Json)
          }
        }
        case _ => Ok(generate("Couldn't find the item object in the request")).withHeaders(Json)
      }
  }

  def deleteOne(collection: String, id: String) = Action {
    implicit request =>

      val dbCollection: MongoCollection = DBConnect.collection(collection)
      val itemToDelete: Option[DBObject] = dbCollection.findOneByID(new ObjectId(id))

      itemToDelete match {
        case Some(foundItemToDelete) => {
          val deletedItem = dbCollection.findAndRemove(foundItemToDelete)
          Ok(generate(DeleteItemResponse(success = true, id = id))).withHeaders(Json)
        }
        case _ => Ok(generate("couldn't find item")).withHeaders(Json)
      }
  }

  def deleteAll(collection: String) = Action {
    implicit request =>
      val dbCollection: MongoCollection = DBConnect.collection(collection)
      dbCollection.drop()
      Ok(generate(("success", true))).withHeaders(Json)
  }

  private def processId(item: DBObject): DBObject = {
    val objectId: ObjectId = item.get("_id").asInstanceOf[ObjectId]
    item.put("_id", Oid(objectId.toString))
    item
  }

  private def getFormParameters(names: String*)(implicit request: Request[AnyContent]): List[Option[String]] = {

    val l = names.toArray[String].toList
    l match {
      case List() => List()
      case _ => List(getFormParameter(l.head)) ::: getFormParameters(l.tail.toArray[String]: _*)
    }
  }

  private def getFormParameter(name: String)(implicit request: Request[AnyContent]): Option[String] = {
    val map: Map[String, Seq[String]] = request.body.asFormUrlEncoded.getOrElse(Map())
    val resultList: Option[Seq[String]] = map.get(name)
    resultList match {
      case None => None
      case Some(list) => Some(list.head)
    }
  }
}
