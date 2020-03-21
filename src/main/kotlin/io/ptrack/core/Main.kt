package io.ptrack.core


import io.ptrack.core.api.api
import org.http4k.server.Jetty
import org.http4k.server.asServer


fun main() {
    api()
    .asServer(Jetty(9000)).start()
}