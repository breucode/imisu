package de.breuco.imisu.api

import de.breuco.imisu.adapters.dns.checkDnsHealth
import de.breuco.imisu.adapters.dns.checkHttpHealth
import de.breuco.imisu.config.DnsServiceConfig
import de.breuco.imisu.config.HttpServiceConfig
import de.breuco.imisu.config.ServiceConfig
import de.breuco.imisu.config.loadedConfig
import org.http4k.contract.ContractRoute
import org.http4k.contract.div
import org.http4k.contract.meta
import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.SERVICE_UNAVAILABLE
import org.http4k.format.Jackson.auto
import org.http4k.lens.Path
import org.http4k.lens.string
import org.http4k.core.Status as HttpStatus

object Services {
  val routes by lazy {
    if (loadedConfig.exposeFullApi) {
      listOf(
        get(),
        Id.get(),
        States.get(),
        States.Id.get()
      )
    } else {
      listOf(States.Id.get())
    }
  }

  const val route = "/services"

  fun get(): ContractRoute {
    val responseLens = Body.auto<List<ServiceConfig>>().toLens()
    fun handler(): HttpHandler = {
      responseLens(
        loadedConfig.services.filter { it.enabled },
        Response(OK)
      )
    }
    return route meta {
      description = "Gets all services, which are available for monitoring"
    } bindContract GET to handler()
  }

  object Id {
    fun get(): ContractRoute {
      fun handler(id: String): HttpHandler =
        { Response(INTERNAL_SERVER_ERROR).body("Not implemented, got $id") }

      return route / Path.string().of("id", "id") meta {
        description = "Gets a service"
      } bindContract GET to ::handler
    }
  }

  object States {
    const val route = Services.route + "/states"

    fun get(): ContractRoute {
      fun handler(): HttpHandler = { Response(INTERNAL_SERVER_ERROR).body("Not implemented") }
      return route meta {
        description =
          "Gets the status of the services, which are available for monitoring. Returns 502, of one of " +
          "the services is unavailable"
      } bindContract GET to handler()
    }

    object Id {
      fun get(): ContractRoute {

        fun handler(id: String): HttpHandler =
          {
            val service = loadedConfig.services.find { it.name == id }

            if (service == null) {
              Response(HttpStatus.NOT_FOUND)
            } else {
              val queryStatus = when (service) {
                is DnsServiceConfig -> checkDnsHealth(service.dnsDomain, service.dnsServer, service.dnsServerPort)
                // PING -> false
                is HttpServiceConfig -> checkHttpHealth(service.httpEndpoint)
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

        return route / Path.string().of("id", "id") meta {
          description = "Gets the status of a service. Returns 502, if service is unavailable"
        } bindContract GET to ::handler
      }
    }
  }
}
