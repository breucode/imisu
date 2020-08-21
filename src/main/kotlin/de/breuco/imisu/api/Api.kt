package de.breuco.imisu.api

import de.breuco.imisu.api.routes.Services
import de.breuco.imisu.config.appConfig
import org.http4k.contract.ContractRenderer
import org.http4k.contract.NoRenderer
import org.http4k.contract.contract
import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.openapi.v3.OpenApi3
import org.http4k.format.Jackson
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes

class Api(private val services: Services) {
  private fun getApiRenderer(): ContractRenderer {
    return if (appConfig.exposeSwagger) {
      OpenApi3(
        ApiInfo("imisu API", "0.0.1", "The API of imisu"),
        Jackson
      )
    } else {
      NoRenderer
    }
  }

  fun routing(): RoutingHttpHandler =
    "/" bind routes(
      contract {
        renderer = getApiRenderer()
        descriptionPath = "swagger.json"
        routes += services.routes
      }
    )
}
