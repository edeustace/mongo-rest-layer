package db

trait ConnectionInitializer {
  def init(mongoUri:String)
}
