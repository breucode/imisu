package de.breuco.imisu

import de.breuco.imisu.api.Api
import de.breuco.imisu.api.routes.Services
import de.breuco.imisu.config.ApplicationConfig
import de.breuco.imisu.service.DnsService
import de.breuco.imisu.service.HttpService
import de.breuco.imisu.service.PingService
import java.nio.file.Paths
import mu.KLogger
import mu.KotlinLogging
import okhttp3.OkHttpClient
import org.http4k.client.OkHttp
import org.http4k.client.PreCannedOkHttpClients
import org.http4k.core.HttpHandler
import org.http4k.server.Netty
import org.http4k.server.asServer
import org.koin.core.context.startKoin
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.minidns.DnsClient

class Application(
  private val api: Api,
  private val appConfig: ApplicationConfig,
  private val logger: KLogger
) {
  fun run() {
    logger.info { "imisu ${appConfig.versions.applicationVersion}" }
    logger.info { "Using config from ${appConfig.configPath.toAbsolutePath()}" }
    logger.info { "Starting application on port ${appConfig.userConfig.serverPort}" }

    if (appConfig.userConfig.exposeFullApi) {
      logger.warn { "Full API is exposed! Everyone can see the internal URLs you have configured!" }
    }

    api.routing().asServer(Netty(appConfig.userConfig.serverPort)).start()

    logger.info { "Application successfully started on port ${appConfig.userConfig.serverPort}" }
  }
}

fun main(args: Array<String>) {
  val koinApplication = startKoin {
    modules(
      module {
        single { Api(get(), get()) }
        single { Services(get(), get(), get(), get()) }
        single { HttpService(get(named("httpClient")), get(named("nonSslValidatingHttpClient"))) }
        single { DnsService(get()) }
        single { PingService() }
        single<HttpHandler>(named("httpClient")) { OkHttp(OkHttpClient.Builder().build()) }
        single<HttpHandler>(named("nonSslValidatingHttpClient")) {
          OkHttp(PreCannedOkHttpClients.insecureOkHttpClient())
        }
        single { DnsClient(null) }
        single {
          ApplicationConfig(
            get(parameters = { parametersOf(ApplicationConfig::class.java.name) }),
            Paths.get(args.firstOrNull() ?: "imisu.conf")
          )
        }
        factory { (name: String) -> KotlinLogging.logger(name) }
        single {
          Application(
            get(),
            get(),
            get(parameters = { parametersOf(Application::class.java.name) })
          )
        }
      }
    )
  }

  val application: Application = koinApplication.koin.get()

  application.run()
}
