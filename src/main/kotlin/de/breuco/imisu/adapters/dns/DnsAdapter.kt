package de.breuco.imisu.adapters.dns

import mu.KotlinLogging
import org.minidns.DnsClient
import org.minidns.record.Record
import java.net.InetAddress

private val logger = KotlinLogging.logger {}

private const val DEFAULT_HOST_NAME = "google.com"
private const val DEFAULT_PORT = 53

fun checkDnsHealth(hostName: String?, ip: String, port: Int?): Boolean {
  val hostNameOrDefault = hostName ?: DEFAULT_HOST_NAME
  val portOrDefault = port ?: DEFAULT_PORT

  return checkDnsHealth(hostNameOrDefault, ip, portOrDefault)
}

fun checkDnsHealth(hostName: String = DEFAULT_HOST_NAME, ip: String, port: Int = DEFAULT_PORT): Boolean {
  val dnsClient = DnsClient(null)

  return try {
    val dnsResult =
      dnsClient.query(hostName, Record.TYPE.A, Record.CLASS.IN, InetAddress.getByName(ip), port)

    logger.debug { dnsResult }

    dnsResult.wasSuccessful()
  } catch (e: Exception) {
    false
  }
}
