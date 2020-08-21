package de.breuco.imisu

import de.breuco.imisu.api.Api
import de.breuco.imisu.api.routes.Services
import de.breuco.imisu.config.appConfig
import de.breuco.imisu.service.DnsService
import de.breuco.imisu.service.HttpService
import mu.KotlinLogging
import okhttp3.OkHttpClient
import org.http4k.client.OkHttp
import org.http4k.core.HttpHandler
import org.http4k.server.Undertow
import org.http4k.server.asServer
import org.koin.core.KoinComponent
import org.koin.core.context.startKoin
import org.koin.core.inject
import org.koin.dsl.module
import org.minidns.DnsClient

class Application : KoinComponent {
  private val logger = KotlinLogging.logger {}
  private val api by inject<Api>()

  fun run() {
    logger.info { "Starting application on port ${appConfig.serverPort}" }

    if (appConfig.exposeFullApi) {
      logger.warn { "Full API is exposed! Everyone can see the internal URLs you have configured!" }
    }

    api.routing().asServer(Undertow(appConfig.serverPort)).start()

    logger.info { "Application successfully started on port ${appConfig.serverPort}" }
  }
}

fun main() {
  startKoin {
    modules(
      module {
        single { Api(get()) }
        single { Services(get(), get()) }
        single { HttpService(get()) }
        single { DnsService(get()) }
        single<HttpHandler> { OkHttp(OkHttpClient.Builder().build()) }
        single { DnsClient(null) }
      }
    )
  }

  Application().run()
}
