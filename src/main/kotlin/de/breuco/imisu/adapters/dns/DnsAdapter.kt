package de.breuco.imisu.adapters.dns

import mu.KotlinLogging
import org.minidns.DnsClient
import org.minidns.dnsmessage.DnsMessage.RESPONSE_CODE.NO_ERROR
import org.minidns.record.Record
import java.net.InetAddress

private val logger = KotlinLogging.logger {}

private const val defaultHostName = "google.com"
private const val defaultPort = 53

fun checkDnsHealth(hostName: String?, ip: String, port: Int?): Boolean {
    val hostNameOrDefault = hostName ?: defaultHostName
    val portOrDefault = port ?: defaultPort

    return checkDnsHealth(hostNameOrDefault, ip, portOrDefault)
}

fun checkDnsHealth(hostName: String = defaultHostName, ip: String, port: Int = defaultPort): Boolean {
    val dnsClient = DnsClient(null)

    return try {
        val dnsResult =
            dnsClient.query(hostName, Record.TYPE.A, Record.CLASS.IN, InetAddress.getByName(ip), port)

        logger.debug { dnsResult }

        dnsResult.responseCode == NO_ERROR
    } catch (t: Throwable) {
        false
    }
}