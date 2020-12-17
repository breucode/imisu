package de.breuco.imisu.service

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.runCatching
import java.net.InetAddress

class PingService {
  fun checkHealth(hostName: String, timeout: Int): Result<HealthCheckResult, Throwable> =
    runCatching {
      InetAddress.getByName(hostName).isReachable(timeout).toHealthCheckResult()
    }
}
