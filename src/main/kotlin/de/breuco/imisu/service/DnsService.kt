package de.breuco.imisu.service

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.runCatching
import de.breuco.imisu.toSuccessIf
import org.minidns.DnsClient
import org.minidns.record.Record
import org.minidns.util.MultipleIoException
import java.net.InetAddress

class DnsService(private val dnsClient: DnsClient) {
  fun checkHealth(dnsDomain: String, host: String, port: Int): Result<HealthCheckResult, Throwable> =
    runCatching {
      dnsClient.query(dnsDomain, Record.TYPE.A, Record.CLASS.IN, InetAddress.getByName(host), port)
        .wasSuccessful()
        .toHealthCheckResult()
    }.toSuccessIf(
      { it is MultipleIoException },
      { HealthCheckFailure(it) }
    )
}
