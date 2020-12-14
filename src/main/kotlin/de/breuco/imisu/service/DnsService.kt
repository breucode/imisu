package de.breuco.imisu.service

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.runCatching
import mu.KLogger
import org.minidns.DnsClient
import org.minidns.record.Record
import org.minidns.util.MultipleIoException
import java.net.InetAddress

class DnsService(private val logger: KLogger, private val dnsClient: DnsClient) {
  fun checkHealth(hostName: String, ip: String, port: Int): Result<Boolean, Throwable> {
    val dnsResult = runCatching {
      dnsClient.query(hostName, Record.TYPE.A, Record.CLASS.IN, InetAddress.getByName(ip), port).wasSuccessful()
    }

    dnsResult.onFailure {
      if (it is MultipleIoException) {
        logger.warn {
          "Error during execution of dns health check for hostName '$hostName', ip '$ip', port '$port'. " +
            "Cause: ${it.message}"
        }
      }
    }

    return dnsResult
  }
}
