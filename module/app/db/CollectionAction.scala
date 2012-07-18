package db

import com.mongodb.casbah.MongoCollection
import com.mongodb.DBObject
import com.codahale.jerkson.Json._
import org.bson.types.ObjectId

trait CollectionAction {
  def list(collection: String): String

  def query(collection: String, query: String, fields: String, limit: Int, offset: Int): String

  def count(collection: String, query: String): String

  def getOne(collection: String, id: String): String

  def createOne(collection: String, data: String): String

  def updateOne(collection: String, id: String, data: String): String

  def deleteOne(collection: String, id: String): String

  def deleteAll(collection: String): String
}

object CollectionAction extends CollectionAction{

  def list(collection: String): String = {
    val dbCollection: MongoCollection = DBConnect.collection(collection)
    generate(dbCollection.map(processId))
  }

  def query(collection: String, query: String, fields: String, limit: Int, offset: Int): String = {
    val dbCollection: MongoCollection = DBConnect.collection(collection)
    val queryObject: DBObject = com.mongodb.util.JSON.parse(query).asInstanceOf[DBObject]
    val fieldsObject: DBObject = com.mongodb.util.JSON.parse(fields).asInstanceOf[DBObject]
    val result = dbCollection.find(queryObject,fieldsObject).skip(offset).limit(limit)
    //TODO: Raise defect in casbah about this:
    //val result = dbCollection.find(queryObject, fieldsObject, offset, limit )
    generate(result.map(processId))
  }

  def count(collection: String, query: String): String = {
    val dbCollection: MongoCollection = DBConnect.collection(collection)
    val queryObject: DBObject = com.mongodb.util.JSON.parse(query).asInstanceOf[DBObject]
    generate(dbCollection.count(queryObject))
  }

  def getOne(collection: String, id: String): String = {
    val dbCollection: MongoCollection = DBConnect.collection(collection)
    val item: Option[DBObject] = dbCollection.findOneByID(new ObjectId(id))
    item match {
      case Some(itemData) => generate(processId(itemData))
      case _ => generate("")
    }
  }

  def createOne(collection: String, data: String): String = {
    val dbCollection: MongoCollection = DBConnect.collection(collection)
    val itemData: DBObject = com.mongodb.util.JSON.parse(data).asInstanceOf[DBObject]
    val result = dbCollection.insert(itemData)
    val processed = processId(itemData)
    generate(processed)
  }

  case class UpdateResult(success:Boolean, message:String)

  def updateOne(collection: String, id: String, data: String): String = {
    val dbCollection: MongoCollection = DBConnect.collection(collection)
    val updateObject: DBObject = com.mongodb.util.JSON.parse(data).asInstanceOf[DBObject]
    val itemToUpdate: Option[DBObject] = dbCollection.findOneByID(new ObjectId(id))

    itemToUpdate match {
      case Some(foundItemToUpdate) => {
        val item: Option[DBObject] = dbCollection.findAndModify(foundItemToUpdate, updateObject)
        getOne(collection, id)
      }
      case _ => generate(UpdateResult(success=false,"can't find object with id: " + id + " in collection: " + collection))
    }
  }

  case class DeleteResult(success: Boolean, id: String)

  def deleteOne(collection: String, id: String): String = {

    val dbCollection: MongoCollection = DBConnect.collection(collection)
    val itemToDelete: Option[DBObject] = dbCollection.findOneByID(new ObjectId(id))

    itemToDelete match {
      case Some(foundItemToDelete) => {
        val deletedItem = dbCollection.findAndRemove(foundItemToDelete)
        generate(DeleteResult(success = true, id))
      }
      case _ => generate(DeleteResult(success = false, id))
    }
  }

  def deleteAll(collection: String): String = {
    val dbCollection: MongoCollection = DBConnect.collection(collection)
    dbCollection.drop()
    generate(DeleteResult(success = true, collection))
  }

  private def processId(item: DBObject): DBObject = {
    val objectId: ObjectId = item.get("_id").asInstanceOf[ObjectId]
    val oid = Map("$oid" -> objectId.toString)
    item.put("_id", oid)
    item
  }
}
