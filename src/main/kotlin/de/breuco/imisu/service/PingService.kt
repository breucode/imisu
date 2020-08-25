package de.breuco.imisu.service

import arrow.core.Either
import de.breuco.imisu.unsafeCatch
import java.net.InetAddress

class PingService {

  fun checkHealth(ip: String, timeout: Int): Either<Throwable, Boolean> {
    return Either.unsafeCatch {
      InetAddress.getByName(ip).isReachable(timeout)
    }
  }
}
