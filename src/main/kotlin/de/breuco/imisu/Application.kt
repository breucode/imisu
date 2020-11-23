package de.breuco.imisu

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import de.breuco.imisu.api.Api
import de.breuco.imisu.api.routes.Services
import de.breuco.imisu.config.ApplicationConfig
import de.breuco.imisu.service.DnsService
import de.breuco.imisu.service.HttpService
import de.breuco.imisu.service.PingService
import mu.KLogger
import mu.KotlinLogging
import okhttp3.OkHttpClient
import org.http4k.client.OkHttp
import org.http4k.core.HttpHandler
import org.http4k.server.Undertow
import org.http4k.server.asServer
import org.koin.core.KoinComponent
import org.koin.core.context.startKoin
import org.koin.core.inject
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import org.minidns.DnsClient
import java.nio.file.Paths
import kotlin.system.exitProcess

class Application : KoinComponent {
  private val api by inject<Api>()
  private val appConfig by inject<ApplicationConfig>()
  private val logger by inject<KLogger> { parametersOf(this.javaClass.name) }

  fun run(printVersion: Boolean) {
    if (printVersion) {
      println("imisu ${appConfig.versions.applicationVersion}")
      exitProcess(0)
    }

    logger.info { "Using config from ${appConfig.configPath.toAbsolutePath()}" }
    logger.info { "Starting application on port ${appConfig.userConfig.serverPort}" }

    if (appConfig.userConfig.exposeFullApi) {
      logger.warn { "Full API is exposed! Everyone can see the internal URLs you have configured!" }
    }

    api.routing().asServer(Undertow(appConfig.userConfig.serverPort)).start()

    logger.info { "Application successfully started on port ${appConfig.userConfig.serverPort}" }
  }
}

private class CliApplicationStarter : CliktCommand(name = "imisu") {
  private val configPath: String by option(help = "Path to the configuration").default("imisu.conf")
  private val displayVersion by option("-v", "--version", help = "Print the version of imisu and exit").flag(
    default = false
  )

  override fun run() {
    startKoin {
      modules(
        module {
          single { Api(get(), get()) }
          single { Services(get(), get(), get(), get()) }
          single { HttpService(get()) }
          single {
            DnsService(
              get(parameters = { parametersOf(DnsService::class.java.name) }),
              get()
            )
          }
          single { PingService() }
          single<HttpHandler> { OkHttp(OkHttpClient.Builder().build()) }
          single { DnsClient(null) }
          single {
            ApplicationConfig(
              get(parameters = { parametersOf(ApplicationConfig::class.java.name) }),
              Paths.get(configPath)
            )
          }
          factory { (name: String) -> KotlinLogging.logger(name) }
        }
      )
    }

    Application().run(displayVersion)
  }
}

fun main(args: Array<String>) = CliApplicationStarter().main(args)
