package de.breuco.imisu.service

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.runCatching
import java.net.InetAddress

class PingService {
  fun checkHealth(ip: String, timeout: Int): Result<Boolean, Throwable> =
    runCatching {
      InetAddress.getByName(ip).isReachable(timeout)
    }
}
