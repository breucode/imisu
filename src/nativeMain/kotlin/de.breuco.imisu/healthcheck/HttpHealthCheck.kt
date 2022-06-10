package de.breuco.imisu.healthcheck

import io.ktor.client.HttpClient
import io.ktor.client.engine.curl.Curl
import io.ktor.client.engine.curl.CurlIllegalStateException
import io.ktor.client.request.request
import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.http.HttpMethod.Companion.Head
import io.ktor.http.isSuccess

private val httpClient = HttpClient(Curl)
private val nonSslValidatingHttpClient = HttpClient(Curl) {
  engine {
    sslVerify = false
  }
}

suspend fun executeHttpHealthCheck(hostName: String, validateSsl: Boolean): Result<HealthCheckResult> =
  runCatching {
    val executingHttpClient =
      if (validateSsl) {
        httpClient
      } else {
        nonSslValidatingHttpClient
      }

    when {
      executingHttpClient.request(hostName) { method = Head }.status.isSuccess() -> HealthCheckSuccess
      executingHttpClient.request(hostName) { method = Get }.status.isSuccess() -> HealthCheckSuccess
      else -> HealthCheckFailure()
    }
  }
    .recoverCatching {
      if (it is CurlIllegalStateException) {
        HealthCheckFailure(it)
      } else {
        throw it
      }
    }
