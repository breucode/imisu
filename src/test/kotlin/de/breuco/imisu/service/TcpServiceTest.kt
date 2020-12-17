package de.breuco.imisu.service

import com.github.michaelbull.result.Err
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.clearAllMocks
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TcpServiceTest {
  private lateinit var underTest: TcpService

  @BeforeEach
  fun beforeEach() {
    underTest = TcpService()
  }

  @AfterEach
  fun afterEach() {
    clearAllMocks()
  }

  @Test
  fun `Tcp error`() {
    val result = underTest.checkHealth("[brokenipv6address", 1234)

    result.shouldBeInstanceOf<Err<*>>()
  }
}
