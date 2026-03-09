package trace4cats.sttp.common

import sttp.model.Uri
import trace4cats.model.AttributeValue.{LongValue, StringValue}
import trace4cats.model.{AttributeValue, SemanticAttributeKeys}

object UriAttributesExtractor {
  // credit : Regular Expressions Cookbook by Steven Levithan, Jan Goyvaerts
  final private val ipv4Regex =
    "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$".r

  // credit : Regular Expressions Cookbook by Steven Levithan, Jan Goyvaerts
  final private val ipv6Regex =
    "^(?:(?:(?:[A-F0-9]{1,4}:){6}|(?=(?:[A-F0-9]{0,4}:){0,6}(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$)(([0-9A-F]{1,4}:){0,5}|:)((:[0-9A-F]{1,4}){1,5}:|:))(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)|(?:[A-F0-9]{1,4}:){7}[A-F0-9]{1,4}|(?=(?:[A-F0-9]{0,4}:){0,7}[A-F0-9]{0,4}$)(([0-9A-F]{1,4}:){1,7}|:)((:[0-9A-F]{1,4}){1,7}|:))$".r

  def uriToAttributes(uri: Uri): Map[String, AttributeValue] = {
    val hostAttribute = uri.host.map { host =>
      val key =
        host.toUpperCase match {
          case ipv4Regex(_*) => SemanticAttributeKeys.remoteServiceIpv4
          case ipv6Regex(_*) => SemanticAttributeKeys.remoteServiceIpv6
          case _ => SemanticAttributeKeys.remoteServiceHostname
        }

      key -> StringValue(host)
    }
    val portAttribute = uri.port.map(port => SemanticAttributeKeys.remoteServicePort -> LongValue(port.toLong))

    Seq(hostAttribute, portAttribute).flatten.toMap
  }
}
