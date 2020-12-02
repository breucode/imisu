package de.breuco.imisu.service

import arrow.core.Either
import de.breuco.imisu.unsafeCatch
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request

class HttpService(
  private val httpClient: HttpHandler,
  private val nonSslValidatingHttpClient: HttpHandler
) {
  fun checkHealth(hostName: String, validateSsl: Boolean): Either<Throwable, Boolean> {
    return Either.unsafeCatch {
      val executingHttpClient = if (validateSsl) {
        httpClient
      } else {
        nonSslValidatingHttpClient
      }

      when {
        executingHttpClient(Request(Method.HEAD, hostName)).status.successful -> true
        executingHttpClient(Request(Method.GET, hostName)).status.successful -> true
        else -> false
      }
    }
  }
}
