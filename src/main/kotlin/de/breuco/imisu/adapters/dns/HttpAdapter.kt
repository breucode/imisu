package de.breuco.imisu.adapters.dns

import arrow.core.Either
import de.breuco.imisu.unsafeCatch
import okhttp3.OkHttpClient
import org.http4k.client.OkHttp
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status

fun checkHttpHealth(hostName: String): Either<Throwable, Boolean> {
  return Either.unsafeCatch {
    val client = OkHttp(OkHttpClient())
    var response = client(Request(Method.OPTIONS, hostName))

    if (response.status == Status.METHOD_NOT_ALLOWED) {
      response = client(Request(Method.GET, hostName))
    }

    response.status.successful
  }
}
