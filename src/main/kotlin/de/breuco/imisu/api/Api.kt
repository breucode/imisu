package de.breuco.imisu.api

import org.http4k.contract.contract
import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.openapi.v3.OpenApi3
import org.http4k.format.Jackson
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes

fun api(): RoutingHttpHandler =
    "/" bind routes(
        contract {
            renderer = OpenApi3(
                ApiInfo("imisu API", "0.0.1", "The API of imisu"),
                Jackson)
            descriptionPath = "swagger.json"
            routes += Services.routes
        }
    )
