package de.breuco.imisu


import de.breuco.imisu.api.api
import org.http4k.server.Undertow
import org.http4k.server.asServer


fun main() {
    api()
    .asServer(Undertow(9000)).start()
}