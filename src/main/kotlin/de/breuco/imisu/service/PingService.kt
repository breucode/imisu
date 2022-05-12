package de.breuco.imisu.service

import java.net.InetAddress

class PingService {
  fun checkHealth(hostName: String, timeout: Int): Result<HealthCheckResult> = runCatching {
    InetAddress.getByName(hostName).isReachable(timeout).toHealthCheckResult()
  }
}
