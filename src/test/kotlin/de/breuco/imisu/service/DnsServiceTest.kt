package de.breuco.imisu.service

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.unwrap
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.reset
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.minidns.DnsClient
import org.minidns.dnsqueryresult.DnsQueryResult
import org.minidns.record.Record
import java.net.InetAddress

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
      .whenever(dnsClientMock).query(hostName, Record.TYPE.A, Record.CLASS.IN, InetAddress.getByName(ip), port)

    doReturn(true)
      .whenever(dnsQueryResultMock).wasSuccessful()

    val result = underTest.checkHealth(hostName, ip, port)

    result.unwrap().shouldBeInstanceOf<HealthCheckSuccess>()

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
      .whenever(dnsClientMock).query(hostName, Record.TYPE.A, Record.CLASS.IN, InetAddress.getByName(ip), port)

    doReturn(false)
      .whenever(dnsQueryResultMock).wasSuccessful()

    val result = underTest.checkHealth(hostName, ip, port)

    result.unwrap().shouldBeInstanceOf<HealthCheckFailure>()

    verify(dnsClientMock)
      .query(hostName, Record.TYPE.A, Record.CLASS.IN, InetAddress.getByName(ip), port)
  }

  @Test
  fun `Error during DNS query`() {
    val hostName = "testHostName"
    val ip = "192.168.0.1"
    val port = 53

    doAnswer { throw Exception() }
      .whenever(dnsClientMock).query(hostName, Record.TYPE.A, Record.CLASS.IN, InetAddress.getByName(ip), port)

    val result = underTest.checkHealth(hostName, ip, port)

    result.shouldBeInstanceOf<Err<*>>()

    verify(dnsClientMock)
      .query(hostName, Record.TYPE.A, Record.CLASS.IN, InetAddress.getByName(ip), port)
  }
}
