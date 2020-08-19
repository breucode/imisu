package de.breuco.imisu.adapters.dns

import arrow.core.Either
import de.breuco.imisu.unsafeCatch
import mu.KotlinLogging
import org.minidns.DnsClient
import org.minidns.record.Record
import java.net.InetAddress

private val logger = KotlinLogging.logger {}

fun checkDnsHealth(hostName: String, ip: String, port: Int): Either<Throwable, Boolean> {
  return Either.unsafeCatch {
    val dnsClient = DnsClient(null)

    val dnsResult =
      dnsClient.query(hostName, Record.TYPE.A, Record.CLASS.IN, InetAddress.getByName(ip), port)

    logger.debug { dnsResult }

    dnsResult.wasSuccessful()
  }
}
