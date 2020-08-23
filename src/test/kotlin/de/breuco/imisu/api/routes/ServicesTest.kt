package de.breuco.imisu.api.routes

import de.breuco.imisu.config.ApplicationConfig
import de.breuco.imisu.service.DnsService
import de.breuco.imisu.service.HttpService
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.NOT_FOUND
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ServicesTest {
  private val appConfigMock = mockk<ApplicationConfig>()
  private val dnsServiceMock = mockk<DnsService>()
  private val httpServiceMock = mockk<HttpService>()

  private lateinit var underTest: Services

  @BeforeEach
  fun beforeEach() {
    underTest = Services(appConfigMock, dnsServiceMock, httpServiceMock)
  }

  @AfterEach
  fun afterEach() {
    confirmVerified(dnsServiceMock, httpServiceMock, appConfigMock)
    clearAllMocks()
  }

  @Nested
  inner class Id {
    @Nested
    inner class Health {
      @Test
      fun `GET service not found`() {
        val serviceId = "testServiceId"

        every { appConfigMock.userConfig.services[serviceId] } returns null

        val route = underTest.Id().Health().get()
        val response = route(Request(GET, "/services/$serviceId/health"))

        response.status shouldBe NOT_FOUND

        verify(exactly = 1) {
          appConfigMock.userConfig.services[serviceId]
        }
      }
    }
  }
}
