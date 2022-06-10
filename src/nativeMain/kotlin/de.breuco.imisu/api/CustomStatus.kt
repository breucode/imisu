package de.breuco.imisu.api

import io.ktor.client.engine.curl.CurlIllegalStateException
import io.ktor.http.HttpStatusCode

val SERVER_IS_DOWN = HttpStatusCode(521, "Server Is Down")
// val ORIGIN_IS_UNREACHABLE = HttpStatusCode(523, "Origin Is Unreachable")
val SSL_HANDSHAKE_FAILED = HttpStatusCode(525, "SSL Handshake Failed")

fun Throwable?.toHttpStatus(): HttpStatusCode =
  when {
    this is CurlIllegalStateException && this.message!!.contains("TLS verification failed") -> SSL_HANDSHAKE_FAILED
    // is MultipleIoException -> ORIGIN_IS_UNREACHABLE
    else -> SERVER_IS_DOWN
  }
