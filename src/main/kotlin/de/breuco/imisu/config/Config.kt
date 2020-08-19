package de.breuco.imisu.config

import com.sksamuel.hoplite.ConfigLoader
import mu.KotlinLogging
import java.nio.file.Path
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger {}

val loadedConfig by lazy {
  ConfigLoader
  val configResult = ConfigLoader.invoke().loadConfig<Config>(Path.of("imisu.conf"))
  lateinit var config: Config
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
  config
}

data class Config(
  val exposeFullApi: Boolean = false,
  val exposeSwagger: Boolean = false,
  val serverPort: Int = 8080,
  val services: List<ServiceConfig>
)

sealed class ServiceConfig {
  abstract val enabled: Boolean
  abstract val name: String
}

data class HttpServiceConfig(
  override val enabled: Boolean,
  override val name: String,
  val httpEndpoint: String,
) : ServiceConfig()

data class DnsServiceConfig(
  override val enabled: Boolean,
  override val name: String,
  val dnsServer: String,
  val dnsServerPort: Int = 53,
  val dnsDomain: String = "google.com"
) : ServiceConfig()
