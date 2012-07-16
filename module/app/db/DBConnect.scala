package db

import com.mongodb.casbah.{MongoConnection, MongoURI, MongoDB, MongoCollection}

object DBConnect extends ConnectionInitializer {

  val connection: Connection = new Connection()

  def init(mongoUri: String) = {
    connection.init(mongoUri)
  }

  def collection(name: String) : MongoCollection = {
    connection.db(name)
  }

  class Connection {

    var db: MongoDB = null

    def init(mongoUri: String) = {

      /*
      val mongoConn = MongoConnection("localhost", 27017)
      db = mongoConn.getDB("content-tagger-dev")
      db.authenticate("test", "test")
       */

      val uri = MongoURI(mongoUri)
      val mongo = MongoConnection(uri)
      db = mongo(uri.database.get)
      db.authenticate(uri.username.get, uri.password.get.foldLeft("")(_ + _.toString))
    }
  }
}


