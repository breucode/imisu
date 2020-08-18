package de.breuco.imisu.config

import com.sksamuel.hoplite.ConfigLoader
import mu.KotlinLogging
import java.nio.file.Path
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger {}

val loadedConfig by lazy {
  val configResult = ConfigLoader.invoke().loadConfig<Config>(Path.of("imisu.conf"))
  var config: Config? = null
  configResult.fold(
    ifValid = {
      config = it
    },
    ifInvalid = {
      logger.error { "Error reading config" }
      logger.error { it.description() }
      logger.error { "Exiting application" }
      exitProcess(1)
    }
  )
  config!!
}

data class Config(
  val exposeFullApi: Boolean = false,
  val exposeSwagger: Boolean = false,
  val serverPort: Int = 8080,
  val services: List<ServiceConfig>
)

data class ServiceConfig(
  val enabled: Boolean,
  val name: String,
  val type: ServiceType,
  val endpoint: String,
  val port: Int?,
  val dnsDomain: String?
)

enum class ServiceType {
  DNS,
  // PING,
  // HTTP
}
