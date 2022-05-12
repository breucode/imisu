package de.breuco.imisu.api.routes

import de.breuco.imisu.api.INVALID_SSL_CERTIFICATE
import de.breuco.imisu.api.ORIGIN_IS_UNREACHABLE
import de.breuco.imisu.api.SERVER_IS_DOWN
import de.breuco.imisu.api.SSL_HANDSHAKE_FAILED
import de.breuco.imisu.api.toHttpStatus
import de.breuco.imisu.config.ApplicationConfig
import de.breuco.imisu.config.DnsServiceConfig
import de.breuco.imisu.config.HttpServiceConfig
import de.breuco.imisu.config.PingServiceConfig
import de.breuco.imisu.config.ServiceConfig
import de.breuco.imisu.service.DnsService
import de.breuco.imisu.service.HealthCheckFailure
import de.breuco.imisu.service.HealthCheckResult
import de.breuco.imisu.service.HealthCheckSuccess
import de.breuco.imisu.service.HttpService
import de.breuco.imisu.service.PingService
import org.http4k.contract.ContractRoute
import org.http4k.contract.div
import org.http4k.contract.meta
import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.HEAD
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.format.Jackson.auto
import org.http4k.lens.Path
import org.http4k.lens.string

class Services(
  private val appConfig: ApplicationConfig,
  private val dnsService: DnsService,
  private val httpService: HttpService,
  private val pingService: PingService,
) {

  val routes by lazy {
    if (appConfig.userConfig.exposeFullApi) {
      listOf(get(), id.get(), id.health.get(), id.health.head())
    } else {
      listOf(id.health.get(), id.health.head())
    }
  }

  private val servicesRoute = "/services"

  private fun get(): ContractRoute {
    val responseLens = Body.auto<Map<String, ServiceConfig>>().toLens()
    fun handler(): HttpHandler = {
      responseLens(
        appConfig.userConfig.services.filter { (_, serviceConfig) -> serviceConfig.enabled },
        Response(OK)
      )
    }
    return servicesRoute meta
      {
        summary = "Gets all services, which are available for monitoring"
        returning(
          OK,
          responseLens to
            mapOf(
              "httpExampleService" to HttpServiceConfig(true, "http://example.org"),
              "dnsExampleService" to DnsServiceConfig(true, "1.1.1.1"),
              "pingExampleService" to PingServiceConfig(true, "1.1.1.1"),
            )
        )
      } bindContract
      GET to
      handler()
  }

  private val id =
    object {
      private val idRoute = servicesRoute / Path.string().of("id", "id")

      fun get(): ContractRoute {
        val responseLens = Body.auto<ServiceConfig>().toLens()
        fun handler() = { id: String ->
          { _: Request ->
            val service =
              appConfig.userConfig.services[id]?.let {
                if (it.enabled) {
                  it
                } else {
                  null
                }
              }

            if (service != null) {
              responseLens(service, Response(OK))
            } else {
              Response(NOT_FOUND)
            }
          }
        }

        return idRoute meta
          {
            summary = "Gets a service"
            returning(OK, responseLens to HttpServiceConfig(true, "http://example.org"))
            returning(NOT_FOUND)
          } bindContract
          GET to
          handler()
      }

      val health =
        object {
          private val healthRoute = idRoute / "health"

          private fun handler() = { id: String, _: String ->
            { _: Request ->
              val service =
                appConfig.userConfig.services[id]?.let {
                  if (it.enabled) {
                    it
                  } else {
                    null
                  }
                }

              if (service == null) {
                Response(NOT_FOUND)
              } else {
                val queryStatus = checkHealth(service)

                queryStatus.fold(
                  onFailure = { Response(INTERNAL_SERVER_ERROR) },
                  onSuccess = {
                    when (it) {
                      is HealthCheckSuccess -> Response(OK)
                      is HealthCheckFailure -> Response(it.cause.toHttpStatus())
                    }
                  }
                )
              }
            }
          }

          val routeSummary = "Gets the health of a service."
          val statusDescriptions =
            arrayOf(
              OK to "the service is healthy",
              SERVER_IS_DOWN to "the service is not healthy",
              ORIGIN_IS_UNREACHABLE to "there was a problem reaching the service",
              INVALID_SSL_CERTIFICATE to "the service is using an invalid SSL certificate",
              SSL_HANDSHAKE_FAILED to "there was a problem with establishing an ssl connection",
              INTERNAL_SERVER_ERROR to "error during health check"
            )

          fun get(): ContractRoute =
            healthRoute meta
              {
                summary = routeSummary
                returning(*statusDescriptions)
              } bindContract
              GET to
              handler()

          fun head(): ContractRoute =
            healthRoute meta
              {
                summary = routeSummary
                returning(*statusDescriptions)
              } bindContract
              HEAD to
              handler()
        }
    }

  private fun checkHealth(service: ServiceConfig): Result<HealthCheckResult> =
    when (service) {
      is DnsServiceConfig ->
        dnsService.checkHealth(service.dnsDomain, service.dnsServer, service.dnsServerPort)
      is PingServiceConfig -> pingService.checkHealth(service.pingServer, service.timeout)
      is HttpServiceConfig -> httpService.checkHealth(service.httpEndpoint, service.validateSsl)
    }
}
