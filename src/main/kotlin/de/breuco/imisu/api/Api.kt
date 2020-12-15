package de.breuco.imisu.api

import de.breuco.imisu.api.routes.Services
import de.breuco.imisu.config.ApplicationConfig
import org.http4k.contract.ContractRenderer
import org.http4k.contract.NoRenderer
import org.http4k.contract.contract
import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.openapi.v3.OpenApi3
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.format.Jackson
import org.http4k.routing.ResourceLoader.Companion.Classpath
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.routing.static

class Api(private val appConfig: ApplicationConfig, private val services: Services) {
  private fun getApiRenderer(): ContractRenderer =
    if (appConfig.userConfig.exposeSwagger) {
      OpenApi3(
        ApiInfo("imisu API", appConfig.versions.applicationVersion, "The API of imisu"),
        Jackson
      )
    } else {
      NoRenderer
    }

  private val swaggerUiPath = "/swagger.json"

  fun routing(): RoutingHttpHandler =
    routes(
      "/swagger-ui" bind GET to {
        Response(Status.FOUND).header("Location", "/swagger-ui/index.html?url=$swaggerUiPath&validatorUrl=")
      },
      "/swagger-ui/" bind static(
        Classpath("META-INF/resources/webjars/swagger-ui/${appConfig.versions.swaggerUiVersion}")
      ),
      contract {
        renderer = getApiRenderer()
        descriptionPath = swaggerUiPath
        routes += services.routes
      }
    )
}
