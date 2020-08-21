package de.breuco.imisu.service

import arrow.core.Either
import de.breuco.imisu.unsafeCatch
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status

class HttpService(private val httpClient: HttpHandler) {
  fun checkHealth(hostName: String): Either<Throwable, Boolean> {
    return Either.unsafeCatch {
      var response = httpClient(Request(Method.OPTIONS, hostName))

      if (response.status == Status.METHOD_NOT_ALLOWED) {
        response = httpClient(Request(Method.GET, hostName))
      }

      response.status.successful
    }
  }
}
