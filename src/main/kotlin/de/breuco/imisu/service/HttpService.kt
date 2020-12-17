package de.breuco.imisu.service

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.runCatching
import de.breuco.imisu.toSuccessIf
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import javax.net.ssl.SSLException

class HttpService(
  private val httpClient: HttpHandler,
  private val nonSslValidatingHttpClient: HttpHandler
) {
  fun checkHealth(hostName: String, validateSsl: Boolean): Result<HealthCheckResult, Throwable> =
    runCatching {
      val executingHttpClient = if (validateSsl) {
        httpClient
      } else {
        nonSslValidatingHttpClient
      }

      when {
        executingHttpClient(Request(Method.HEAD, hostName)).status.successful -> HealthCheckSuccess
        executingHttpClient(Request(Method.GET, hostName)).status.successful -> HealthCheckSuccess
        else -> HealthCheckFailure()
      }
      // Replace with recoverIf, when next kotlin-result release is out
    }.toSuccessIf(
      { it is SSLException },
      { HealthCheckFailure(it) }
    )
}
