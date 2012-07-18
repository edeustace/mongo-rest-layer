package brokers

import play.api.mvc.{AnyContent, Action, RequestHeader}

trait RequestBroker{
  /**
   * Matches a request with a result string.
   * @param request the request to handle
   * @return either the string result or None if it can't handle the request
   */
  def handle(request:RequestHeader) : Option[String]
  def handle2(request:RequestHeader) : Option[Action[AnyContent]]
}
