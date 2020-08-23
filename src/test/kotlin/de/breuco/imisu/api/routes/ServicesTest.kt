package de.breuco.imisu.api.routes

import arrow.core.Either
import de.breuco.imisu.config.ApplicationConfig
import de.breuco.imisu.config.DnsServiceConfig
import de.breuco.imisu.config.HttpServiceConfig
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
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.SERVICE_UNAVAILABLE
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
      fun `GET Http service health check successful`() {
        val serviceId = "testServiceId"
        val httpEndpoint = "http://example.org"

        every { appConfigMock.userConfig.services[serviceId] } returns HttpServiceConfig(true, httpEndpoint)
        every { httpServiceMock.checkHealth(httpEndpoint) } returns Either.right(true)

        val route = underTest.Id().Health().get()
        val response = route(Request(GET, "/services/$serviceId/health"))

        response.status shouldBe OK

        verify(exactly = 1) {
          appConfigMock.userConfig.services[serviceId]
          httpServiceMock.checkHealth(httpEndpoint)
        }
      }

      @Test
      fun `GET Dns service health check successful`() {
        val serviceId = "testServiceId"
        val dnsServer = "testDnsServer"

        every { appConfigMock.userConfig.services[serviceId] } returns DnsServiceConfig(true, dnsServer)
        every { dnsServiceMock.checkHealth("example.org", dnsServer, 53) } returns Either.right(true)

        val route = underTest.Id().Health().get()
        val response = route(Request(GET, "/services/$serviceId/health"))

        response.status shouldBe OK

        verify(exactly = 1) {
          appConfigMock.userConfig.services[serviceId]
          dnsServiceMock.checkHealth("example.org", dnsServer, 53)
        }
      }

      @Test
      fun `GET Http service health check unsuccessful`() {
        val serviceId = "testServiceId"
        val httpEndpoint = "http://example.org"

        every { appConfigMock.userConfig.services[serviceId] } returns HttpServiceConfig(true, httpEndpoint)
        every { httpServiceMock.checkHealth(httpEndpoint) } returns Either.right(false)

        val route = underTest.Id().Health().get()
        val response = route(Request(GET, "/services/$serviceId/health"))

        response.status shouldBe SERVICE_UNAVAILABLE

        verify(exactly = 1) {
          appConfigMock.userConfig.services[serviceId]
          httpServiceMock.checkHealth(httpEndpoint)
        }
      }

      @Test
      fun `GET Http service health check error`() {
        val serviceId = "testServiceId"
        val httpEndpoint = "http://example.org"

        every { appConfigMock.userConfig.services[serviceId] } returns HttpServiceConfig(true, httpEndpoint)
        every { httpServiceMock.checkHealth(httpEndpoint) } returns Either.left(Exception())

        val route = underTest.Id().Health().get()
        val response = route(Request(GET, "/services/$serviceId/health"))

        response.status shouldBe INTERNAL_SERVER_ERROR

        verify(exactly = 1) {
          appConfigMock.userConfig.services[serviceId]
          httpServiceMock.checkHealth(httpEndpoint)
        }
      }

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
