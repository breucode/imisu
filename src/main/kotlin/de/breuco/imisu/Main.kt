package de.breuco.imisu

import de.breuco.imisu.api.api
import de.breuco.imisu.config.loadedConfig
import mu.KotlinLogging
import org.http4k.server.Undertow
import org.http4k.server.asServer

private val logger = KotlinLogging.logger {}

fun main() {
  logger.info { "Initializing" }

  api().asServer(Undertow(loadedConfig.serverPort)).start()

  logger.info { "Started on port ${loadedConfig.serverPort}" }
}
