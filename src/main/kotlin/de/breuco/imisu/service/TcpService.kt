package de.breuco.imisu.service

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.runCatching
import de.breuco.imisu.toSuccessIf
import java.net.ConnectException
import java.net.Socket

class TcpService {
  fun checkHealth(hostName: String, port: Int): Result<HealthCheckResult, Throwable> =
    runCatching {
      Socket(hostName, port).use { }
      HealthCheckSuccess
    }.toSuccessIf(
      { it is ConnectException },
      { HealthCheckFailure(it) }
    )
}
