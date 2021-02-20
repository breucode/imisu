package de.breuco.imisu.service

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.unwrap
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
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
      val inetAddress = mock<InetAddress>()
      it.`when`<Any> { InetAddress.getByName(pingAddress) }
        .thenReturn(inetAddress)

      doReturn(true).whenever(inetAddress).isReachable(timeout)

      val result = underTest.checkHealth(pingAddress, timeout = timeout)

      result.unwrap().shouldBeInstanceOf<HealthCheckSuccess>()
    }
  }

  @Test
  fun `Ping unsuccessful`() {
    val pingAddress = "192.168.0.1"
    val timeout = 1000

    Mockito.mockStatic(InetAddress::class.java).use {
      val inetAddress = mock<InetAddress>()
      it.`when`<Any> { InetAddress.getByName(pingAddress) }
        .thenReturn(inetAddress)

      doReturn(false).whenever(inetAddress).isReachable(timeout)

      val result = underTest.checkHealth(pingAddress, timeout = timeout)

      result.unwrap().shouldBeInstanceOf<HealthCheckFailure>()
    }
  }

  @Test
  fun `Ping error`() {
    val pingAddress = "192.168.0.1"
    val timeout = 1000

    Mockito.mockStatic(InetAddress::class.java).use {
      val inetAddress = mock<InetAddress>()
      it.`when`<Any> { InetAddress.getByName(pingAddress) }
        .thenReturn(inetAddress)

      doAnswer { Exception() }.whenever(inetAddress).isReachable(timeout)

      val result = underTest.checkHealth(pingAddress, timeout = timeout)

      result.shouldBeInstanceOf<Err<*>>()
    }
  }
}
