package de.breuco.imisu.service

import java.net.InetAddress
import org.minidns.DnsClient
import org.minidns.record.Record
import org.minidns.util.MultipleIoException

class DnsService(private val dnsClient: DnsClient) {
  fun checkHealth(dnsDomain: String, host: String, port: Int): Result<HealthCheckResult> =
    runCatching {
        dnsClient
          .query(dnsDomain, Record.TYPE.A, Record.CLASS.IN, InetAddress.getByName(host), port)
          .wasSuccessful()
          .toHealthCheckResult()
      }
      .recoverCatching {
        if (it is MultipleIoException) {
          HealthCheckFailure(it)
        } else {
          throw it
        }
      }
}
