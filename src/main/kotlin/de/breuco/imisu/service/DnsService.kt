package de.breuco.imisu.service

import arrow.core.Either
import de.breuco.imisu.unsafeCatch
import mu.KLogger
import org.minidns.DnsClient
import org.minidns.record.Record
import org.minidns.util.MultipleIoException
import java.net.InetAddress

class DnsService(private val logger: KLogger, private val dnsClient: DnsClient) {
  fun checkHealth(hostName: String, ip: String, port: Int): Either<Throwable, Boolean> {
    val dnsResult = Either.unsafeCatch {
      dnsClient.query(hostName, Record.TYPE.A, Record.CLASS.IN, InetAddress.getByName(ip), port).wasSuccessful()
    }

    dnsResult.mapLeft {
      if (it is MultipleIoException) {
        logger.warn {
          "Error during execution of dns health check for hostName '$hostName', ip '$ip$, port '$port'. " +
            "Cause: ${it.message}"
        }
      }
    }

    return dnsResult
  }
}
