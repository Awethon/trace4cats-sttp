package trace4cats.sttp.client4

import sttp.client4.GenericRequest
import trace4cats.model.AttributeValue
import trace4cats.sttp.common.UriAttributesExtractor

object SttpRequest {
  def toAttributes[T, R](req: GenericRequest[T, R]): Map[String, AttributeValue] =
    UriAttributesExtractor.uriToAttributes(req.uri)
}
