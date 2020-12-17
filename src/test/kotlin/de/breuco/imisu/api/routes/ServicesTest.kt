package de.breuco.imisu.api.routes

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import de.breuco.imisu.api.Api
import de.breuco.imisu.api.SERVER_IS_DOWN
import de.breuco.imisu.api.SSL_HANDSHAKE_FAILED
import de.breuco.imisu.config.ApplicationConfig
import de.breuco.imisu.config.DnsServiceConfig
import de.breuco.imisu.config.HttpServiceConfig
import de.breuco.imisu.config.PingServiceConfig
import de.breuco.imisu.config.UserConfig
import de.breuco.imisu.config.Versions
import de.breuco.imisu.service.DnsService
import de.breuco.imisu.service.HealthCheckFailure
import de.breuco.imisu.service.HealthCheckSuccess
import de.breuco.imisu.service.HttpService
import de.breuco.imisu.service.PingService
import de.breuco.imisu.service.TcpService
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.http4k.core.Method
import org.http4k.core.Method.GET
import org.http4k.core.Method.HEAD
import org.http4k.core.Request
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.kotest.shouldHaveBody
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import javax.net.ssl.SSLException

class ServicesTest {
  private val appConfigMock = mockk<ApplicationConfig>()
  private val userConfigMock = mockk<UserConfig>()
  private val dnsServiceMock = mockk<DnsService>()
  private val httpServiceMock = mockk<HttpService>()
  private val pingServiceMock = mockk<PingService>()
  private val tcpServiceMock = mockk<TcpService>()

  private lateinit var api: Api

  @BeforeEach
  fun beforeEach() {
    every { appConfigMock.userConfig } returns userConfigMock
    every { userConfigMock.exposeFullApi } returns true
    every { userConfigMock.exposeSwagger } returns false
    every { appConfigMock.versions } returns Versions("appVersion", "swaggerUiVersion")
    api = Api(appConfigMock, Services(appConfigMock, dnsServiceMock, httpServiceMock, pingServiceMock, tcpServiceMock))
  }

  @AfterEach
  fun afterEach() {
    verify(exactly = 1) {
      userConfigMock.exposeFullApi
      userConfigMock.exposeSwagger
    }
    confirmVerified(dnsServiceMock, httpServiceMock, tcpServiceMock, userConfigMock)
    clearAllMocks()
  }

  @Test
  fun `GET one service`() {
    val serviceId = "testServiceId"
    val httpEndpoint = "http://example.org"

    every { userConfigMock.services } returns mapOf(
      serviceId to HttpServiceConfig(true, httpEndpoint)
    )

    val route = api.routing()
    val response = route(Request(GET, "/services"))

    response.status shouldBe OK
    response shouldHaveBody """{"$serviceId":{"enabled":true,"httpEndpoint":"$httpEndpoint","validateSsl":true}}"""

    verify(exactly = 1) {
      userConfigMock.services
    }
  }

  @Test
  fun `GET service disabled`() {
    val serviceId = "testServiceId"
    val httpEndpoint = "http://example.org"

    every { userConfigMock.services } returns mapOf(
      serviceId to HttpServiceConfig(false, httpEndpoint)
    )

    val route = api.routing()
    val response = route(Request(GET, "/services"))

    response.status shouldBe OK
    response shouldHaveBody "{}"

    verify(exactly = 1) {
      userConfigMock.services
    }
  }

  @Nested
  inner class Id {
    @Test
    fun `GET ServiceConfig found`() {
      val serviceId = "testServiceId"
      val httpEndpoint = "http://example.org"

      every { userConfigMock.services[serviceId] } returns HttpServiceConfig(true, httpEndpoint)

      val route = api.routing()
      val response = route(Request(GET, "/services/$serviceId"))

      response.status shouldBe OK
      response shouldHaveBody """{"enabled":true,"httpEndpoint":"$httpEndpoint","validateSsl":true}"""

      verify(exactly = 1) {
        userConfigMock.services[serviceId]
      }
    }

    @Test
    fun `GET ServiceConfig disabled`() {
      val serviceId = "testServiceId"
      val httpEndpoint = "http://example.org"

      every { userConfigMock.services[serviceId] } returns HttpServiceConfig(false, httpEndpoint)

      val route = api.routing()
      val response = route(Request(GET, "/services/$serviceId"))

      response.status shouldBe NOT_FOUND

      verify(exactly = 1) {
        userConfigMock.services[serviceId]
      }
    }

    @Test
    fun `GET ServiceConfig not found`() {
      val serviceId = "testServiceId"

      every { userConfigMock.services[serviceId] } returns null

      val route = api.routing()
      val response = route(Request(GET, "/services/$serviceId"))

      response.status shouldBe NOT_FOUND

      verify(exactly = 1) {
        userConfigMock.services[serviceId]
      }
    }

    @Nested
    inner class Health {
      @Test
      fun `GET Http service health check successful`() {
        `Http service health check successful`(GET)
      }

      @Test
      fun `HEAD Http service health check successful`() {
        `Http service health check successful`(HEAD)
      }

      private fun `Http service health check successful`(method: Method) {
        val serviceId = "testServiceId"
        val httpEndpoint = "http://example.org"

        every { userConfigMock.services[serviceId] } returns HttpServiceConfig(true, httpEndpoint)
        every { httpServiceMock.checkHealth(httpEndpoint, true) } returns Ok(HealthCheckSuccess)

        val route = api.routing()
        val response = route(Request(method, "/services/$serviceId/health"))

        response.status shouldBe OK

        verify(exactly = 1) {
          userConfigMock.services[serviceId]
          httpServiceMock.checkHealth(httpEndpoint, true)
        }
      }

      @Test
      fun `GET service disabled`() {
        `service disabled`(GET)
      }

      @Test
      fun `HEAD service disabled`() {
        `service disabled`(HEAD)
      }

      private fun `service disabled`(method: Method) {
        val serviceId = "testServiceId"
        val httpEndpoint = "http://example.org"

        every { userConfigMock.services[serviceId] } returns HttpServiceConfig(false, httpEndpoint)

        val route = api.routing()
        val response = route(Request(method, "/services/$serviceId/health"))

        response.status shouldBe NOT_FOUND

        verify(exactly = 1) {
          userConfigMock.services[serviceId]
        }
      }

      @Test
      fun `GET Dns service health check successful`() {
        `Dns service health check successful`(GET)
      }

      @Test
      fun `HEAD Dns service health check successful`() {
        `Dns service health check successful`(HEAD)
      }

      private fun `Dns service health check successful`(method: Method) {
        val serviceId = "testServiceId"
        val dnsServer = "testDnsServer"

        every { userConfigMock.services[serviceId] } returns DnsServiceConfig(true, dnsServer)
        every { dnsServiceMock.checkHealth("example.org", dnsServer, 53) } returns Ok(HealthCheckSuccess)

        val route = api.routing()
        val response = route(Request(method, "/services/$serviceId/health"))

        response.status shouldBe OK

        verify(exactly = 1) {
          userConfigMock.services[serviceId]
          dnsServiceMock.checkHealth("example.org", dnsServer, 53)
        }
      }

      @Test
      fun `GET Ping service health check successful`() {
        `Ping service health check successful`(GET)
      }

      @Test
      fun `HEAD Ping service health check successful`() {
        `Ping service health check successful`(HEAD)
      }

      private fun `Ping service health check successful`(method: Method) {
        val serviceId = "testServiceId"
        val pingServer = "192.168.0.1"

        every { userConfigMock.services[serviceId] } returns PingServiceConfig(true, pingServer)
        every { pingServiceMock.checkHealth(pingServer, 1000) } returns Ok(HealthCheckSuccess)

        val route = api.routing()
        val response = route(Request(method, "/services/$serviceId/health"))

        response.status shouldBe OK

        verify(exactly = 1) {
          userConfigMock.services[serviceId]
          pingServiceMock.checkHealth(pingServer, 1000)
        }
      }

      @Test
      fun `GET service health check unsuccessful`() {
        `Http service health check unsuccessful`(GET)
      }

      @Test
      fun `HEAD service health check unsuccessful`() {
        `Http service health check unsuccessful`(HEAD)
      }

      private fun `Http service health check unsuccessful`(method: Method) {
        val serviceId = "testServiceId"
        val httpEndpoint = "http://example.org"

        every { userConfigMock.services[serviceId] } returns HttpServiceConfig(true, httpEndpoint)
        every { httpServiceMock.checkHealth(httpEndpoint, true) } returns Ok(HealthCheckFailure())

        val route = api.routing()
        val response = route(Request(method, "/services/$serviceId/health"))

        response.status shouldBe SERVER_IS_DOWN

        verify(exactly = 1) {
          userConfigMock.services[serviceId]
          httpServiceMock.checkHealth(httpEndpoint, true)
        }
      }

      @Test
      fun `GET Http service health check error`() {
        `Http service health check error`(GET)
      }

      @Test
      fun `HEAD Http service health check error`() {
        `Http service health check error`(HEAD)
      }

      private fun `Http service health check error`(method: Method) {
        val serviceId = "testServiceId"
        val httpEndpoint = "http://example.org"

        every { userConfigMock.services[serviceId] } returns HttpServiceConfig(true, httpEndpoint)
        every { httpServiceMock.checkHealth(httpEndpoint, true) } returns Err(Exception())

        val route = api.routing()
        val response = route(Request(method, "/services/$serviceId/health"))

        response.status shouldBe INTERNAL_SERVER_ERROR

        verify(exactly = 1) {
          userConfigMock.services[serviceId]
          httpServiceMock.checkHealth(httpEndpoint, true)
        }
      }

      @Test
      fun `GET Http service health check ssl error`() {
        `Http service health check ssl error`(GET)
      }

      @Test
      fun `HEAD Http service health check ssl error`() {
        `Http service health check ssl error`(HEAD)
      }

      private fun `Http service health check ssl error`(method: Method) {
        val serviceId = "testServiceId"
        val httpEndpoint = "http://example.org"

        every { userConfigMock.services[serviceId] } returns HttpServiceConfig(true, httpEndpoint)
        every { httpServiceMock.checkHealth(httpEndpoint, true) } returns Ok(HealthCheckFailure(SSLException("")))

        val route = api.routing()
        val response = route(Request(method, "/services/$serviceId/health"))

        response.status shouldBe SSL_HANDSHAKE_FAILED

        verify(exactly = 1) {
          userConfigMock.services[serviceId]
          httpServiceMock.checkHealth(httpEndpoint, true)
        }
      }

      @Test
      fun `GET service not found`() {
        `service not found`(GET)
      }

      @Test
      fun `HEAD service not found`() {
        `service not found`(HEAD)
      }

      private fun `service not found`(method: Method) {
        val serviceId = "testServiceId"

        every { userConfigMock.services[serviceId] } returns null

        val route = api.routing()
        val response = route(Request(method, "/services/$serviceId/health"))

        response.status shouldBe NOT_FOUND

        verify(exactly = 1) {
          userConfigMock.services[serviceId]
        }
      }
    }
  }
}
