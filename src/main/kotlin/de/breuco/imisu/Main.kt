package de.breuco.imisu


import de.breuco.imisu.api.api
import org.http4k.server.Jetty
import org.http4k.server.asServer


fun main() {
    api()
    .asServer(Jetty(9000)).start()
}