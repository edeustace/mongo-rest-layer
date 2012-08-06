package db

import com.mongodb.casbah.{MongoCursor, MongoCollection}
import com.mongodb.{WriteResult, DBObject}

import org.bson.types.ObjectId
import com.sun.tools.javac.main.OptionName
import com.codahale.jerkson.Json.generate

/**
 * A set of db actions that takes a string and returns a json string.
 */
trait JsonCollectionAction {
  def list(collection: String): String

  def query(collection: String, query: String, fields: String, limit: Int, offset: Int): String

  def count(collection: String, query: String): String

  def getOne(collection: String, id: String): String

  def createOne(collection: String, data: String): String

  def updateOne(collection: String, id: String, data: String): String

  def deleteOne(collection: String, id: String): String

  def deleteAll(collection: String): String
}

/**
 * A set of db actions that take strings and return either DBObjects or MongoCollections
 */
trait CollectionAction {
  def list(collection: String): Option[MongoCollection]

  def query(collection: String, query: String, fields: String, limit: Int, offset: Int): Option[List[DBObject]]

  def count(collection: String, query: String): Option[Long]

  def getOne(collection: String, id: String): Option[DBObject]

  def createOne(collection: String, data: String): Option[DBObject]

  def updateOne(collection: String, id: String, data: String): Option[DBObject]

  def deleteOne(collection: String, id: String): Option[String]

  def deleteAll(collection: String): Option[String]
}

object JsonCollectionAction extends JsonCollectionAction{
  def list(collection: String): String = {
    CollectionAction.list(collection) match {
      case Some(dbCollection) => generate(dbCollection.map(processId))
      case _ => ""
    }
  }

  def query(collection: String, query: String, fields: String, limit: Int, offset: Int): String = {
    CollectionAction.query(collection,query,fields,limit,offset) match {
      case Some(dbCollection) => generate(dbCollection.map(processId))
      case _ => ""
    }
  }

  def count(collection: String, query: String): String = {
   CollectionAction.count(collection,query) match {
     case Some(value) => generate(value)
     case _ => ""
   }
  }

  def getOne(collection: String, id: String): String = {
    CollectionAction.getOne(collection, id) match {
      case Some(dbo) => generate(processId(dbo))
      case _ => ""
    }
  }

  def createOne(collection: String, data: String): String = {
   CollectionAction.createOne(collection, data) match {
     case Some(dbo) => generate(processId(dbo))
     case _ => ""
   }
  }

  def updateOne(collection: String, id: String, data: String): String = {
   CollectionAction.updateOne(collection,id,data) match {
     case Some(dbo) => generate(processId(dbo))
     case _ => ""
   }
  }

  case class DeleteResult(success: Boolean, id: String)

  def deleteOne(collection: String, id: String): String = {
   CollectionAction.deleteOne(collection,id) match {
     case Some(deletedId) => generate(DeleteResult(success = true,id))
     case _ => generate(DeleteResult(success = false,""))
   }
  }

  def deleteAll(collection: String): String = {
    CollectionAction.deleteAll(collection) match {
      case Some(deletedCollection) => generate(deletedCollection)
      case _ => ""
    }
  }

  private def processId(item: DBObject): DBObject = {
    val objectId: ObjectId = item.get("_id").asInstanceOf[ObjectId]
    val oid = Map("$oid" -> objectId.toString)
    item.put("_id", oid)
    item
  }
}
object CollectionAction extends CollectionAction{

  def list(collection: String): Option[MongoCollection] = {
    val dbCollection: MongoCollection = DBConnect.collection(collection)
    dbCollection match {
      case null => None
      case _ => Some(dbCollection)
    }
  }

  def query(collection: String, query: String, fields: String, limit: Int, offset: Int): Option[List[DBObject]] = {
    val dbCollection: MongoCollection = DBConnect.collection(collection)
    val queryObject: DBObject = com.mongodb.util.JSON.parse(query).asInstanceOf[DBObject]
    val fieldsObject: DBObject = com.mongodb.util.JSON.parse(fields).asInstanceOf[DBObject]
    val result : scala.Iterator[DBObject] = dbCollection.find(queryObject,fieldsObject).skip(offset).limit(limit)

    result match {
      case null => None
      case _ => Some(result.toList)
    }
  }

  def count(collection: String, query: String): Option[Long] = {
    val dbCollection: MongoCollection = DBConnect.collection(collection)
    val queryObject: DBObject = com.mongodb.util.JSON.parse(query).asInstanceOf[DBObject]
    Some(dbCollection.count(queryObject))
  }

  def getOne(collection: String, id: String): Option[DBObject] = {
    val dbCollection: MongoCollection = DBConnect.collection(collection)
    val item: Option[DBObject] = dbCollection.findOneByID(new ObjectId(id))
    item match {
      case Some(itemData) => Some(itemData)
      case _ => None
    }
  }

  def createOne(collection: String, data: String): Option[DBObject] = {
    val dbCollection: MongoCollection = DBConnect.collection(collection)
    val itemData: DBObject = com.mongodb.util.JSON.parse(data).asInstanceOf[DBObject]
    val result : WriteResult = dbCollection.insert(itemData)
    Some(itemData)
  }

  def updateOne(collection: String, id: String, data: String): Option[DBObject] = {
    val dbCollection: MongoCollection = DBConnect.collection(collection)
    val updateObject: DBObject = com.mongodb.util.JSON.parse(data).asInstanceOf[DBObject]
    val itemToUpdate: Option[DBObject] = dbCollection.findOneByID(new ObjectId(id))

    itemToUpdate match {
      case Some(foundItemToUpdate) => {
        val item: Option[DBObject] = dbCollection.findAndModify(foundItemToUpdate, updateObject)
        getOne(collection, id)
      }
      case _ => None
    }
  }


  def deleteOne(collection: String, id: String): Option[String] = {

    val dbCollection: MongoCollection = DBConnect.collection(collection)
    val itemToDelete: Option[DBObject] = dbCollection.findOneByID(new ObjectId(id))

    itemToDelete match {
      case Some(foundItemToDelete) => {
        val deletedItem = dbCollection.findAndRemove(foundItemToDelete)
        deletedItem match {
          case Some(dbCollectionT) => Some(id)
          case _ => None
        }
      }
      case _ => None
    }
  }

  def deleteAll(collection: String): Option[String] = {
    val dbCollection: MongoCollection = DBConnect.collection(collection)

    try{
      dbCollection.drop()
    } catch {
      case e : Exception => println("Error: " + e.getMessage ); None
      case _ => None
    }
    Some(collection)
  }

}
