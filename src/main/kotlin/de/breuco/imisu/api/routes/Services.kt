package de.breuco.imisu.api.routes

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
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.SERVICE_UNAVAILABLE
import org.http4k.format.Jackson.auto
import org.http4k.lens.Path
import org.http4k.lens.string
import org.http4k.core.Status as HttpStatus

class Services(
  private val appConfig: ApplicationConfig,
  private val dnsService: DnsService,
  private val httpService: HttpService
) {
  val routes by lazy {
    if (appConfig.userConfig.exposeFullApi) {
      listOf(
        get(),
        Id().get(),
        Id().Health().get(),
        Health().get()
      )
    } else {
      listOf(Id().Health().get())
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
      description = "Gets all services, which are available for monitoring"
    } bindContract GET to handler()
  }

  inner class Id {
    private val route = this@Services.route / Path.string().of("id", "id")

    fun get(): ContractRoute {
      fun handler() = { id: String ->
        { _: Request ->
          Response(INTERNAL_SERVER_ERROR).body("Not implemented, got $id")
        }
      }

      return route meta {
        description = "Gets a service"
      } bindContract GET to handler()
    }

    inner class Health {
      private val route = this@Id.route / "health"

      fun get(): ContractRoute {
        fun handler() = { id: String, _: String ->
          { _: Request ->
            val service = appConfig.userConfig.services[id]

            if (service == null) {
              Response(HttpStatus.NOT_FOUND)
            } else {
              val queryStatus = when (service) {
                is DnsServiceConfig -> dnsService.checkHealth(
                  service.dnsDomain,
                  service.dnsServer,
                  service.dnsServerPort
                )
                // PING -> false
                is HttpServiceConfig -> httpService.checkHealth(service.httpEndpoint)
              }

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
          description = "Gets the status of a service. Returns 502, if service is unavailable"
        } bindContract GET to handler()
      }
    }
  }

  inner class Health {
    private val route = this@Services.route + "/health"

    fun get(): ContractRoute {
      fun handler(): HttpHandler = { Response(INTERNAL_SERVER_ERROR).body("Not implemented") }
      return route meta {
        description =
          "Gets the status of the services, which are available for monitoring. Returns 502, of one of " +
          "the services is unavailable"
      } bindContract GET to handler()
    }
  }
}
