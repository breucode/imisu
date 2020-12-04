package de.breuco.imisu.api.routes

import arrow.core.Either
import de.breuco.imisu.api.INVALID_SSL_CERTIFICATE
import de.breuco.imisu.config.ApplicationConfig
import de.breuco.imisu.config.DnsServiceConfig
import de.breuco.imisu.config.HttpServiceConfig
import de.breuco.imisu.config.PingServiceConfig
import de.breuco.imisu.config.ServiceConfig
import de.breuco.imisu.service.DnsService
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
import org.http4k.core.Status.Companion.SERVICE_UNAVAILABLE
import org.http4k.format.Jackson.auto
import org.http4k.lens.Path
import org.http4k.lens.string
import javax.net.ssl.SSLPeerUnverifiedException

class Services(
  private val appConfig: ApplicationConfig,
  private val dnsService: DnsService,
  private val httpService: HttpService,
  private val pingService: PingService
) {
  val routes by lazy {
    val health = Health()
    val id = Id()

    if (appConfig.userConfig.exposeFullApi) {
      listOf(
        get(),
        health.get(),
        health.head(),
        id.get(),
        id.health.get(),
        id.health.head()
      )
    } else {
      listOf(
        health.get(),
        health.head(),
        id.health.get(),
        id.health.head()
      )
    }
  }

  private val route = "/services"

  private fun get(): ContractRoute {
    val responseLens = Body.auto<Map<String, ServiceConfig>>().toLens()
    fun handler(): HttpHandler = {
      responseLens(
        appConfig.userConfig.services.filter { (_, serviceConfig) -> serviceConfig.enabled },
        Response(OK)
      )
    }
    return route meta {
      summary = "Gets all services, which are available for monitoring"
      returning(
        OK,
        responseLens to mapOf(
          "httpExampleService" to HttpServiceConfig(true, "http://example.org"),
          "dnsExampleService" to DnsServiceConfig(true, "1.1.1.1"),
          "pingExampleService" to PingServiceConfig(true, "1.1.1.1")
        )
      )
    } bindContract GET to handler()
  }

  private inner class Id {
    private val route = this@Services.route / Path.string().of("id", "id")
    val health = Health()

    fun get(): ContractRoute {
      val responseLens = Body.auto<ServiceConfig>().toLens()
      fun handler() = { id: String ->
        { _: Request ->
          val service = appConfig.userConfig.services[id]?.let {
            if (it.enabled) {
              it
            } else {
              null
            }
          }

          if (service != null) {
            responseLens(
              service,
              Response(OK)
            )
          } else {
            Response(NOT_FOUND)
          }
        }
      }

      return route meta {
        summary = "Gets a service"
        returning(OK, responseLens to HttpServiceConfig(true, "http://example.org"))
        returning(NOT_FOUND)
      } bindContract GET to handler()
    }

    inner class Health {
      private val route = this@Id.route / "health"

      private fun handler() = { id: String, _: String ->
        { _: Request ->
          val service = appConfig.userConfig.services[id]?.let {
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
              ifLeft = {
                if (it is SSLPeerUnverifiedException) {
                  Response(INVALID_SSL_CERTIFICATE)
                } else {
                  Response(INTERNAL_SERVER_ERROR)
                }
              },
              ifRight = {
                if (it) {
                  Response(OK)
                } else {
                  Response(SERVICE_UNAVAILABLE)
                }
              }
            )
          }
        }
      }

      fun get(): ContractRoute {
        return route meta {
          summary = "Gets the health of a service. Returns 503, if service is unavailable"
          returning(
            OK to "service is healthy",
            SERVICE_UNAVAILABLE to "service is not healthy",
            INTERNAL_SERVER_ERROR to "error during health check",
            INVALID_SSL_CERTIFICATE to "the service is using an invalid SSL certificate"
          )
        } bindContract GET to handler()
      }

      fun head(): ContractRoute {
        return route meta {
          summary = "Gets the health of a service. Returns 503, if service is unavailable"
          returning(
            OK to "service is healthy",
            SERVICE_UNAVAILABLE to "service is not healthy",
            INTERNAL_SERVER_ERROR to "error during health check",
            INVALID_SSL_CERTIFICATE to "the service is using an invalid SSL certificate"
          )
        } bindContract HEAD to handler()
      }
    }
  }

  private inner class Health {
    private val route = this@Services.route + "/health"

    private fun handler(): HttpHandler = {
      val healthOfAllServices = appConfig.userConfig.services
        .filter { (_, serviceConfig) -> serviceConfig.enabled }
        .values
        .map {
          checkHealth(it)
        }

      when {
        healthOfAllServices.any { it.isLeft() } -> Response(INTERNAL_SERVER_ERROR)
        healthOfAllServices.any { either -> either.exists { it.not() } } -> Response(SERVICE_UNAVAILABLE)
        else -> Response(OK)
      }
    }

    fun get(): ContractRoute {
      return route meta {
        summary =
          "Gets the health of the services, which are available for monitoring. Returns 503, if one of " +
          "the services is unavailable"
        returning(
          OK to "All services are healthy",
          SERVICE_UNAVAILABLE to "At least one of the services is not healthy",
          INTERNAL_SERVER_ERROR to "At least one error during health checks"
        )
      } bindContract GET to handler()
    }

    fun head(): ContractRoute {
      return route meta {
        summary =
          "Gets the health of the services, which are available for monitoring. Returns 503, if one of " +
          "the services is unavailable"
        returning(
          OK to "All services are healthy",
          SERVICE_UNAVAILABLE to "At least one of the services is not healthy",
          INTERNAL_SERVER_ERROR to "At least one error during health checks"
        )
      } bindContract HEAD to handler()
    }
  }

  private fun checkHealth(service: ServiceConfig): Either<Throwable, Boolean> {
    return when (service) {
      is DnsServiceConfig -> dnsService.checkHealth(
        service.dnsDomain,
        service.dnsServer,
        service.dnsServerPort
      )
      is PingServiceConfig -> pingService.checkHealth(service.pingServer, service.timeout)
      is HttpServiceConfig -> httpService.checkHealth(service.httpEndpoint, service.validateSsl)
    }
  }
}
