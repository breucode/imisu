package de.breuco.imisu

import de.breuco.imisu.api.toHttpStatus
import de.breuco.imisu.config.UserConfig
import de.breuco.imisu.healthcheck.executeHttpHealthCheck
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

fun main(args: Array<String>) {
  val configFilePath = args.firstOrNull() ?: "imisu.conf"

  val userConfig = readTextFile(configFilePath).let {
    if (it.isBlank()) exitWithMessage("Config file not found or empty", 1)

    runCatching { Json.decodeFromString<UserConfig>(it) }
      .onFailure {
        exitWithMessage("Config file could not be parsed", 2)
      }.getOrThrow()
  }

  embeddedServer(CIO, port = userConfig.serverPort) {
    routing {
      userConfig.healthChecks.httpHealthChecks.map { healthCheckConfig ->
        get("/${healthCheckConfig.name}") {
          executeHttpHealthCheck(healthCheckConfig.target, healthCheckConfig.validateSsl).fold(
            onSuccess = { call.respond(status = HttpStatusCode.OK, "") },
            onFailure = { call.respond(status = it.toHttpStatus(), "") }
          )
        }
      }
    }
  }.start(wait = true)
}
