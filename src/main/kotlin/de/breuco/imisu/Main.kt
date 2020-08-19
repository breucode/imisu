package de.breuco.imisu

import de.breuco.imisu.api.api
import de.breuco.imisu.config.loadedConfig
import mu.KotlinLogging
import org.http4k.server.Undertow
import org.http4k.server.asServer

private val logger = KotlinLogging.logger {}

fun main() {
  logger.info { "Starting application on port ${loadedConfig.serverPort}" }

  if (loadedConfig.exposeFullApi) {
    logger.warn { "Full API is exposed! Everyone can see the internal URLs you have configured!" }
  }

  api().asServer(Undertow(loadedConfig.serverPort)).start()

  logger.info { "Application successfully started on port ${loadedConfig.serverPort}" }
}
