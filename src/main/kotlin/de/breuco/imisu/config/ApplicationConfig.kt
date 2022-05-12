package de.breuco.imisu.config

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ConfigSource
import com.sksamuel.hoplite.addFileSource
import com.sksamuel.hoplite.sources.ConfigFilePropertySource
import java.nio.file.Path
import kotlin.system.exitProcess
import mu.KLogger

private val FORBIDDEN_SERVICE_NAMES = setOf("health")

class ApplicationConfig(private val logger: KLogger, val configPath: Path) {
  val versions =
    ConfigLoaderBuilder.default()
      .addPropertySource(
        ConfigFilePropertySource(ConfigSource.ClasspathSource("/versions.properties"))
      )
      .build()
      .loadConfig<Versions>()
      .fold(
        ifInvalid = {
          logger.error {
            """
              |Error during application startup. Please report this to https://github.com/breucode/imisu/issues
              |${it.description()}
            """.trimMargin()
          }
          exitProcess(1)
        },
        ifValid = { it }
      )

  val userConfig =
    ConfigLoaderBuilder.default()
      .addFileSource(configPath.toFile())
      .build()
      .loadConfig<UserConfig>()
      .fold(
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
        },
        ifValid = { userConfig ->
          val forbiddenServices = userConfig.services.keys.filter { it in FORBIDDEN_SERVICE_NAMES }

          if (forbiddenServices.isNotEmpty()) {
            logger.error {
              """
                |Error in config configuration
                |----------------------------------------------------------------------------
                |Service name(s) '${forbiddenServices.joinToString()}' are not allowed
                |----------------------------------------------------------------------------
                |Exiting application
              """.trimMargin()
            }
            exitProcess(1)
          }

          userConfig
        }
      )
}

data class Versions(val applicationVersion: String, val swaggerUiVersion: String)

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
  val validateSsl: Boolean = true
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
