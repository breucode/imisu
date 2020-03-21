package de.breuco.imisu.adapters.dns

import org.minidns.DnsClient
import org.minidns.dnsmessage.DnsMessage.RESPONSE_CODE.NO_ERROR
import org.minidns.record.Record
import java.net.InetAddress

fun queryDns(hostName: String = "google.com", ip: String, port: Int = 53): Boolean {
    val dnsClient = DnsClient(null)

    return try {
        val dnsResult =
            dnsClient.query(hostName, Record.TYPE.A, Record.CLASS.IN, InetAddress.getByName(ip), port)

        dnsResult.responseCode == NO_ERROR
    } catch (t: Throwable) {
        false
    }
}