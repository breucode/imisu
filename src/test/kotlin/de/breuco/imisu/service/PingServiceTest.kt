package de.breuco.imisu.service

import io.kotest.matchers.result.shouldBeFailure
import io.kotest.matchers.types.shouldBeInstanceOf
import java.net.InetAddress
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

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
      val inetAddress = mock<InetAddress>()
      it.`when`<Any> { InetAddress.getByName(pingAddress) }.thenReturn(inetAddress)

      doReturn(true).whenever(inetAddress).isReachable(timeout)

      val result = underTest.checkHealth(pingAddress, timeout = timeout)

      result.getOrThrow().shouldBeInstanceOf<HealthCheckSuccess>()
    }
  }

  @Test
  fun `Ping unsuccessful`() {
    val pingAddress = "192.168.0.1"
    val timeout = 1000

    Mockito.mockStatic(InetAddress::class.java).use {
      val inetAddress = mock<InetAddress>()
      it.`when`<Any> { InetAddress.getByName(pingAddress) }.thenReturn(inetAddress)

      doReturn(false).whenever(inetAddress).isReachable(timeout)

      val result = underTest.checkHealth(pingAddress, timeout = timeout)

      result.getOrThrow().shouldBeInstanceOf<HealthCheckFailure>()
    }
  }

  @Test
  fun `Ping error`() {
    val pingAddress = "192.168.0.1"
    val timeout = 1000

    Mockito.mockStatic(InetAddress::class.java).use {
      val inetAddress = mock<InetAddress>()
      it.`when`<Any> { InetAddress.getByName(pingAddress) }.thenReturn(inetAddress)

      doAnswer { Exception() }.whenever(inetAddress).isReachable(timeout)

      val result = underTest.checkHealth(pingAddress, timeout = timeout)

      result.shouldBeFailure()
    }
  }
}
