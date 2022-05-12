package de.breuco.imisu.service

import io.kotest.matchers.result.shouldBeFailure
import io.kotest.matchers.types.shouldBeInstanceOf
import java.net.InetAddress
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.minidns.DnsClient
import org.minidns.dnsqueryresult.DnsQueryResult
import org.minidns.record.Record
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

class DnsServiceTest {

  private val dnsClientMock = mock<DnsClient>()

  private lateinit var underTest: DnsService

  @BeforeEach
  fun beforeEach() {
    underTest = DnsService(dnsClientMock)
  }

  @AfterEach
  fun afterEach() {
    verifyNoMoreInteractions(dnsClientMock)
    reset(dnsClientMock)
  }

  @Test
  fun `DNS query successful`() {
    val hostName = "testHostName"
    val ip = "192.168.0.1"
    val port = 53

    val dnsQueryResultMock = mock<DnsQueryResult>()

    doReturn(dnsQueryResultMock)
      .whenever(dnsClientMock)
      .query(hostName, Record.TYPE.A, Record.CLASS.IN, InetAddress.getByName(ip), port)

    doReturn(true).whenever(dnsQueryResultMock).wasSuccessful()

    val result = underTest.checkHealth(hostName, ip, port)

    result.getOrThrow().shouldBeInstanceOf<HealthCheckSuccess>()

    verify(dnsClientMock)
      .query(hostName, Record.TYPE.A, Record.CLASS.IN, InetAddress.getByName(ip), port)
  }

  @Test
  fun `DNS query unsuccessful`() {
    val hostName = "testHostName"
    val ip = "192.168.0.1"
    val port = 53

    val dnsQueryResultMock = mock<DnsQueryResult>()

    doReturn(dnsQueryResultMock)
      .whenever(dnsClientMock)
      .query(hostName, Record.TYPE.A, Record.CLASS.IN, InetAddress.getByName(ip), port)

    doReturn(false).whenever(dnsQueryResultMock).wasSuccessful()

    val result = underTest.checkHealth(hostName, ip, port)

    result.getOrThrow().shouldBeInstanceOf<HealthCheckFailure>()

    verify(dnsClientMock)
      .query(hostName, Record.TYPE.A, Record.CLASS.IN, InetAddress.getByName(ip), port)
  }

  @Test
  fun `Error during DNS query`() {
    val hostName = "testHostName"
    val ip = "192.168.0.1"
    val port = 53

    doAnswer { throw Exception() }
      .whenever(dnsClientMock)
      .query(hostName, Record.TYPE.A, Record.CLASS.IN, InetAddress.getByName(ip), port)

    val result = underTest.checkHealth(hostName, ip, port)

    result.shouldBeFailure()

    verify(dnsClientMock)
      .query(hostName, Record.TYPE.A, Record.CLASS.IN, InetAddress.getByName(ip), port)
  }
}
