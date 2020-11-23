package de.breuco.imisu.service

import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.mockk.clearAllMocks
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import mu.KLogger
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.minidns.DnsClient
import org.minidns.dnsqueryresult.DnsQueryResult
import org.minidns.record.Record
import java.net.InetAddress

class DnsServiceTest {

  private val loggerMock = mockk<KLogger>(relaxed = true)

  private val dnsClientMock = mockk<DnsClient>()

  private lateinit var underTest: DnsService

  @BeforeEach
  fun beforeEach() {
    underTest = DnsService(loggerMock, dnsClientMock)
  }

  @AfterEach
  fun afterEach() {
    confirmVerified(dnsClientMock)
    clearAllMocks()
  }

  @Test
  fun `DNS query successful`() {
    val hostName = "testHostName"
    val ip = "192.168.0.1"
    val port = 53

    val dnsQueryResultMock = mockk<DnsQueryResult>()

    every {
      dnsClientMock.query(hostName, Record.TYPE.A, Record.CLASS.IN, InetAddress.getByName(ip), port)
    } returns dnsQueryResultMock

    every { dnsQueryResultMock.wasSuccessful() } returns true

    val result = underTest.checkHealth(hostName, ip, port)

    result.shouldBeRight(true)

    verify(exactly = 1) {
      dnsClientMock.query(hostName, Record.TYPE.A, Record.CLASS.IN, InetAddress.getByName(ip), port)
    }
  }

  @Test
  fun `DNS query unsuccessful`() {
    val hostName = "testHostName"
    val ip = "192.168.0.1"
    val port = 53

    val dnsQueryResultMock = mockk<DnsQueryResult>()

    every {
      dnsClientMock.query(hostName, Record.TYPE.A, Record.CLASS.IN, InetAddress.getByName(ip), port)
    } returns dnsQueryResultMock

    every { dnsQueryResultMock.wasSuccessful() } returns false

    val result = underTest.checkHealth(hostName, ip, port)

    result.shouldBeRight(false)

    verify(exactly = 1) {
      dnsClientMock.query(hostName, Record.TYPE.A, Record.CLASS.IN, InetAddress.getByName(ip), port)
    }
  }

  @Test
  fun `Error during DNS query`() {
    val hostName = "testHostName"
    val ip = "192.168.0.1"
    val port = 53

    every {
      dnsClientMock.query(hostName, Record.TYPE.A, Record.CLASS.IN, InetAddress.getByName(ip), port)
    } throws Exception()

    val result = underTest.checkHealth(hostName, ip, port)

    result.shouldBeLeft()

    verify(exactly = 1) {
      dnsClientMock.query(hostName, Record.TYPE.A, Record.CLASS.IN, InetAddress.getByName(ip), port)
    }
  }
}
