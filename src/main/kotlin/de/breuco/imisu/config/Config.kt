package de.breuco.imisu.config

import com.sksamuel.hoplite.ConfigLoader
import mu.KLogger
import java.nio.file.Path
import kotlin.system.exitProcess

class ApplicationConfig(private val logger: KLogger, val configPath: Path) {
  val userConfig = {
    val configResult = ConfigLoader.invoke().loadConfig<Config>(configPath)
    lateinit var config: Config
    configResult.fold(
      ifValid = {
        config = it
      },
      ifInvalid = {
        logger.error {
          """
            |Error reading config
            |----------------------------------------------------------------------------
            |${it.description()}
            |----------------------------------------------------------------------------
            |Exiting application
          """.trimMargin()
        }
        exitProcess(1)
      }
    )
    config
  }.invoke()
}

data class Config(
  val exposeFullApi: Boolean = false,
  val exposeSwagger: Boolean = false,
  val serverPort: Int = 8080,
  val services: Map<String, ServiceConfig>
)

sealed class ServiceConfig {
  abstract val enabled: Boolean
}

data class HttpServiceConfig(
  override val enabled: Boolean,
  val httpEndpoint: String,
) : ServiceConfig()

data class DnsServiceConfig(
  override val enabled: Boolean,
  val dnsServer: String,
  val dnsServerPort: Int = 53,
  val dnsDomain: String = "example.org"
) : ServiceConfig()
