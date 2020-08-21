package de.breuco.imisu.service

import arrow.core.Either
import de.breuco.imisu.unsafeCatch
import mu.KotlinLogging
import org.minidns.DnsClient
import org.minidns.record.Record
import java.net.InetAddress

class DnsService(private val dnsClient: DnsClient) {
  companion object {
    private val logger = KotlinLogging.logger {}
  }

  fun checkHealth(hostName: String, ip: String, port: Int): Either<Throwable, Boolean> {
    return Either.unsafeCatch {
      val dnsResult =
        dnsClient.query(hostName, Record.TYPE.A, Record.CLASS.IN, InetAddress.getByName(ip), port)

      logger.debug { dnsResult }

      dnsResult.wasSuccessful()
    }
  }
}
