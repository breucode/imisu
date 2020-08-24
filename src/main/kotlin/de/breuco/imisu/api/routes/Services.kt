package de.breuco.imisu.api.routes

import arrow.core.Either
import de.breuco.imisu.config.ApplicationConfig
import de.breuco.imisu.config.DnsServiceConfig
import de.breuco.imisu.config.HttpServiceConfig
import de.breuco.imisu.config.ServiceConfig
import de.breuco.imisu.service.DnsService
import de.breuco.imisu.service.HttpService
import org.http4k.contract.ContractRoute
import org.http4k.contract.div
import org.http4k.contract.meta
import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.SERVICE_UNAVAILABLE
import org.http4k.format.Jackson.auto
import org.http4k.lens.Path
import org.http4k.lens.string

class Services(
  private val appConfig: ApplicationConfig,
  private val dnsService: DnsService,
  private val httpService: HttpService
) {
  val routes by lazy {
    if (appConfig.userConfig.exposeFullApi) {
      listOf(
        get(),
        Health().get(),
        Id().get(),
        Id().Health().get(),
      )
    } else {
      listOf(
        Id().Health().get(),
        Health().get()
      )
    }
  }

  private val route = "/services"

  fun get(): ContractRoute {
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
          "dnsExampleService" to DnsServiceConfig(true, "8.8.8.8")
        )
      )
    } bindContract GET to handler()
  }

  inner class Id {
    private val route = this@Services.route / Path.string().of("id", "id")

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

      fun get(): ContractRoute {
        fun handler() = { id: String, _: String ->
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
                ifLeft = { Response(INTERNAL_SERVER_ERROR) },
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

        return route meta {
          summary = "Gets the health of a service. Returns 502, if service is unavailable"
          returning(
            OK to "service is healthy",
            SERVICE_UNAVAILABLE to "service is not healthy",
            INTERNAL_SERVER_ERROR to "error during health check"
          )
        } bindContract GET to handler()
      }
    }
  }

  inner class Health {
    private val route = this@Services.route + "/health"

    fun get(): ContractRoute {
      fun handler(): HttpHandler = {
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
      return route meta {
        summary =
          "Gets the health of the services, which are available for monitoring. Returns 502, if one of " +
          "the services is unavailable"
        returning(
          OK to "All services are healthy",
          SERVICE_UNAVAILABLE to "At least one of the services is not healthy",
          INTERNAL_SERVER_ERROR to "At least one error during health checks"
        )
      } bindContract GET to handler()
    }
  }

  fun checkHealth(service: ServiceConfig): Either<Throwable, Boolean> {
    return when (service) {
      is DnsServiceConfig -> dnsService.checkHealth(
        service.dnsDomain,
        service.dnsServer,
        service.dnsServerPort
      )
      // PING -> false
      is HttpServiceConfig -> httpService.checkHealth(service.httpEndpoint)
    }
  }
}
