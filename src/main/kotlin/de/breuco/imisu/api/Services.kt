package de.breuco.imisu.api

import de.breuco.imisu.adapters.dns.queryDns
import org.http4k.contract.ContractRoute
import org.http4k.contract.div
import org.http4k.contract.meta
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.lens.Path
import org.http4k.lens.string
import org.http4k.core.Status as HttpStatus

object Services {
    val routes = listOf(
        get(),
        Id.get(),
        Status.get(),
        Status.Id.get()
    )

    const val route = "/services"

    fun get(): ContractRoute {
        fun handler(): HttpHandler = { Response(HttpStatus.INTERNAL_SERVER_ERROR).body("Not implemented") }
        return route meta {
            description = "Gets all services, which are available for monitoring"
        } bindContract GET to handler()
    }

    object Id {
        fun get(): ContractRoute {
            fun handler(id: String): HttpHandler =
                { Response(HttpStatus.INTERNAL_SERVER_ERROR).body("Not implemented, got $id") }

            return route / Path.string().of("id", "id") meta {
                description = "Gets a service"
            } bindContract GET to ::handler
        }
    }

    object Status {
        const val route = Services.route + "/status"

        fun get(): ContractRoute {
            fun handler(): HttpHandler = { Response(HttpStatus.INTERNAL_SERVER_ERROR).body("Not implemented") }
            return route meta {
                description = "Gets the status of the services, which are available for monitoring. Returns 502, of one of the services is unavailable"

            } bindContract GET to handler()
        }

        object Id {
            fun get(): ContractRoute {

                fun handler(id: String): HttpHandler =
                    {
                        val result = queryDns(ip = "192.168.1.2")
                        Response(HttpStatus.INTERNAL_SERVER_ERROR)
                    }

                return route / Path.string().of("id", "id") meta {
                    description = "Gets the status of a service. Returns 502, if service is unavailable"
                } bindContract GET to ::handler
            }
        }
    }
}