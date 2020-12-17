package de.breuco.imisu.api

import org.http4k.core.Status
import org.minidns.util.MultipleIoException
import javax.net.ssl.SSLException
import javax.net.ssl.SSLPeerUnverifiedException

val SERVER_IS_DOWN = Status(521, "Server Is Down")
val ORIGIN_IS_UNREACHABLE = Status(523, "Origin Is Unreachable")
val SSL_HANDSHAKE_FAILED = Status(525, "SSL Handshake Failed")
val INVALID_SSL_CERTIFICATE = Status(526, "Invalid SSL certificate")

fun Throwable?.toHttpStatus(): Status =
  when (this) {
    is SSLPeerUnverifiedException -> INVALID_SSL_CERTIFICATE
    is SSLException -> SSL_HANDSHAKE_FAILED
    is MultipleIoException -> ORIGIN_IS_UNREACHABLE
    else -> SERVER_IS_DOWN
  }
