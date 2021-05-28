package de.breuco.imisu.service

import java.net.ConnectException
import java.net.Socket

class TcpService {
  fun checkHealth(hostName: String, port: Int): Result<HealthCheckResult> =
    runCatching {
      Socket(hostName, port).use { }
      HealthCheckSuccess
    }.recoverCatching {
      if (it is ConnectException) {
        HealthCheckFailure(it)
      } else {
        throw it
      }
    }
}
