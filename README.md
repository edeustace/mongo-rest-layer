#mongo-rest-layer
A simple mongo rest layer for a Play! 2 app.

## Usage
### Add the dependency
Add the dependency to project/Build.scala

    val appDependencies = Seq(
      "mongo-rest-layer" %% "mongo-rest-layer" % "0.1-SNAPSHOT",
      "org.mongodb" %% "casbah" % "2.4.0")

### Add Global class
Create a Global object in your application. This will intercept the route
requests and will see if it can handle them.

    object Global extends GlobalSettings{
   
       val mongoRestLayer = new MongoRestLayer("/mongo-rest", mongoUri)

       override def onRouteRequest(request:RequestHeader) : Option[Handler] = {
          val handler : Option[Handler] = mongoRestLayer.handlerFor(request)
          handler match {
            case Some(h) => handler
            case _ =>
                Play.maybeApplication.flatMap(_.routes.flatMap { router =>
                    router.handlerFor(request)
                })
           }
      }
    }

### Make some calls
    play run

    curl http://localhost:9000/mongo-rest/itemData                 
    []%
    
    curl http://localhost:9000/mongo-rest/itemData --request PUT --data
    "item={'name':'ed'}"
    {"name":"ed","_id":{"oid":"5003fe86300482920bbec0c7"}}%

    curl http://localhost:9000/mongo-rest/itemData                                          
    [{"_id":{"oid":"5003fe86300482920bbec0c7"},"name":"ed"}]%  

