package de.breuco.imisu.service

import io.kotest.matchers.result.shouldBeFailure
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.lang.RuntimeException
import java.net.InetAddress

class PingServiceTest {
  private lateinit var underTest: PingService

  @BeforeEach
  fun beforeEach() {
    underTest = PingService()
  }

  @Test
  fun `Ping successful`() {
    val pingAddress = "192.168.0.1"
    val timeout = 1000

    Mockito.mockStatic(InetAddress::class.java).use {
      val inetAddressMock = mock<InetAddress>()

      it.`when`<Any> {
        InetAddress.getByName(pingAddress)
      }.thenReturn(inetAddressMock)

      doReturn(true).whenever(inetAddressMock).isReachable(timeout)

      val result = underTest.checkHealth(pingAddress, timeout = timeout)

      result.getOrThrow().shouldBeInstanceOf<HealthCheckSuccess>()
    }
  }

  @Test
  fun `Ping unsuccessful`() {
    val pingAddress = "192.168.0.1"
    val timeout = 1000

    Mockito.mockStatic(InetAddress::class.java).use {
      val inetAddressMock = mock<InetAddress>()

      it.`when`<Any> {
        InetAddress.getByName(pingAddress)
      }.thenReturn(inetAddressMock)

      doReturn(false).whenever(inetAddressMock).isReachable(timeout)

      val result = underTest.checkHealth(pingAddress, timeout = timeout)

      result.getOrThrow().shouldBeInstanceOf<HealthCheckFailure>()
    }
  }

  @Test
  fun `Ping error`() {
    val pingAddress = "192.168.0.1"
    val timeout = 1000

    Mockito.mockStatic(InetAddress::class.java).use {
      val inetAddressMock = mock<InetAddress>()

      it.`when`<Any> {
        InetAddress.getByName(pingAddress)
      }.thenReturn(inetAddressMock)

      doThrow(RuntimeException()).whenever(inetAddressMock).isReachable(timeout)

      val result = underTest.checkHealth(pingAddress, timeout = timeout)

      result.shouldBeFailure()
    }
  }
}
