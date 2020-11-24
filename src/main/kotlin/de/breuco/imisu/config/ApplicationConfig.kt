package de.breuco.imisu.config

import com.sksamuel.hoplite.ConfigFilePropertySource
import com.sksamuel.hoplite.ConfigLoader
import com.sksamuel.hoplite.ConfigSource
import mu.KLogger
import java.nio.file.Path
import kotlin.system.exitProcess

class ApplicationConfig(private val logger: KLogger, val configPath: Path) {
  val versions = run {
    val configResult = ConfigLoader.Builder()
      .addPropertySource(ConfigFilePropertySource(ConfigSource.ClasspathSource("/versions.properties")))
      .build()
      .loadConfig<Versions>()
    lateinit var config: Versions
    configResult.fold(
      ifValid = {
        config = it
      },
      ifInvalid = {
        logger.error {
          """
          |Error during application startup. Please report this to https://github.com/breucode/imisu/issues
          |${it.description()}
        """.trimMargin()
        }
        exitProcess(1)
      }
    )
    config
  }

  val userConfig by lazy {
    val configResult = ConfigLoader().loadConfig<UserConfig>(configPath)
    lateinit var config: UserConfig
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
  }
}

data class Versions(
  val applicationVersion: String,
  val swaggerUiVersion: String
)

data class UserConfig(
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

data class PingServiceConfig(
  override val enabled: Boolean,
  val pingServer: String,
  val timeout: Int = 1000
) : ServiceConfig()
