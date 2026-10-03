package org.phone_client.service

import scala.io.Source

object UIService {
  def getIndexTemplateHtml : String = {
    val path = "templates/index.html"

    val stream = getClass.getClassLoader.getResourceAsStream(path)
    require(stream != null, s"Template '${path}' not found in resources.")
    val source = Source.fromInputStream(stream, "UTF-8")
    try source.mkString finally source.close()
  }
}
