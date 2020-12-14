package de.breuco.imisu.service

import com.github.michaelbull.result.unwrap
import de.breuco.imisu.isError
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.InetAddress

class PingServiceTest {
  private lateinit var underTest: PingService

  @BeforeEach
  fun beforeEach() {
    underTest = PingService()
  }

  @AfterEach
  fun afterEach() {
    clearAllMocks()
  }

  @Test
  fun `Ping successful`() {
    mockkStatic(InetAddress::class)

    val pingAddress = "192.168.0.1"
    val timeout = 1000
    every { InetAddress.getByName(pingAddress).isReachable(timeout) } returns true

    val result = underTest.checkHealth(pingAddress, timeout = timeout)

    result.unwrap() shouldBe true
  }

  @Test
  fun `Ping unsuccessful`() {
    mockkStatic(InetAddress::class)

    val pingAddress = "192.168.0.1"
    val timeout = 1000
    every { InetAddress.getByName(pingAddress).isReachable(timeout) } returns false

    val result = underTest.checkHealth(pingAddress, timeout = timeout)

    result.unwrap() shouldBe false
  }

  @Test
  fun `Ping error`() {
    mockkStatic(InetAddress::class)

    val pingAddress = "192.168.0.1"
    val timeout = 1000
    every { InetAddress.getByName(pingAddress).isReachable(timeout) } throws Exception()

    val result = underTest.checkHealth(pingAddress, timeout = timeout)

    result.isError() shouldBe true
  }
}
