package de.breuco.imisu.api.routes

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import de.breuco.imisu.api.Api
import de.breuco.imisu.api.INVALID_SSL_CERTIFICATE
import de.breuco.imisu.config.ApplicationConfig
import de.breuco.imisu.config.DnsServiceConfig
import de.breuco.imisu.config.HttpServiceConfig
import de.breuco.imisu.config.PingServiceConfig
import de.breuco.imisu.config.UserConfig
import de.breuco.imisu.config.Versions
import de.breuco.imisu.service.DnsService
import de.breuco.imisu.service.HttpService
import de.breuco.imisu.service.PingService
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
import org.http4k.core.Status.Companion.SERVICE_UNAVAILABLE
import org.http4k.kotest.shouldHaveBody
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import javax.net.ssl.SSLPeerUnverifiedException

class ServicesTest {
  private val appConfigMock = mockk<ApplicationConfig>()
  private val userConfigMock = mockk<UserConfig>()
  private val dnsServiceMock = mockk<DnsService>()
  private val httpServiceMock = mockk<HttpService>()
  private val pingServiceMock = mockk<PingService>()

  private lateinit var api: Api

  @BeforeEach
  fun beforeEach() {
    every { appConfigMock.userConfig } returns userConfigMock
    every { userConfigMock.exposeFullApi } returns true
    every { userConfigMock.exposeSwagger } returns false
    every { appConfigMock.versions } returns Versions("appVersion", "swaggerUiVersion")
    api = Api(appConfigMock, Services(appConfigMock, dnsServiceMock, httpServiceMock, pingServiceMock))
  }

  @AfterEach
  fun afterEach() {
    verify(exactly = 1) {
      userConfigMock.exposeFullApi
      userConfigMock.exposeSwagger
    }
    confirmVerified(dnsServiceMock, httpServiceMock, userConfigMock)
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
        every { httpServiceMock.checkHealth(httpEndpoint, true) } returns Ok(true)

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
        every { dnsServiceMock.checkHealth("example.org", dnsServer, 53) } returns Ok(true)

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
        every { pingServiceMock.checkHealth(pingServer, 1000) } returns Ok(true)

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
        every { httpServiceMock.checkHealth(httpEndpoint, true) } returns Ok(false)

        val route = api.routing()
        val response = route(Request(method, "/services/$serviceId/health"))

        response.status shouldBe SERVICE_UNAVAILABLE

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
        every { httpServiceMock.checkHealth(httpEndpoint, true) } returns Err(SSLPeerUnverifiedException(""))

        val route = api.routing()
        val response = route(Request(method, "/services/$serviceId/health"))

        response.status shouldBe INVALID_SSL_CERTIFICATE

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

  @Nested
  inner class Health {
    @Test
    fun `GET no service configured`() {
      `no service configured`(GET)
    }

    @Test
    fun `HEAD no service configured`() {
      `no service configured`(HEAD)
    }

    private fun `no service configured`(method: Method) {
      every { userConfigMock.services } returns emptyMap()

      val route = api.routing()
      val response = route(Request(method, "/services/health"))

      response.status shouldBe OK

      verify(exactly = 1) {
        userConfigMock.services
      }
    }

    @Test
    fun `GET ssl error`() {
      `ssl error`(GET)
    }

    @Test
    fun `HEAD ssl error`() {
      `ssl error`(HEAD)
    }

    private fun `ssl error`(method: Method) {
      val sslErrorServiceConfig = HttpServiceConfig(true, "http://example.org")
      every { userConfigMock.services } returns mapOf(
        "sslErrorService" to sslErrorServiceConfig
      )

      every { httpServiceMock.checkHealth("http://example.org", true) } returns Err(SSLPeerUnverifiedException(""))

      val route = api.routing()
      val response = route(Request(method, "/services/health"))

      response.status shouldBe SERVICE_UNAVAILABLE

      verify(exactly = 1) {
        userConfigMock.services
        httpServiceMock.checkHealth("http://example.org", true)
      }
    }

    @Test
    fun `GET error, unavailable and success`() {
      `error, unavailable and success`(GET)
    }

    @Test
    fun `HEAD error, unavailable and success`() {
      `error, unavailable and success`(HEAD)
    }

    private fun `error, unavailable and success`(method: Method) {
      val errorServiceConfig = HttpServiceConfig(true, "http://example.org")
      val unavailableServiceConfig = HttpServiceConfig(true, "http://example.com")
      val successServiceConfig = HttpServiceConfig(true, "http://example.net")
      every { userConfigMock.services } returns mapOf(
        "errorService" to errorServiceConfig,
        "unavailableService" to unavailableServiceConfig,
        "successService" to successServiceConfig
      )

      every { httpServiceMock.checkHealth("http://example.org", true) } returns Err(Exception())
      every { httpServiceMock.checkHealth("http://example.com", true) } returns Ok(false)
      every { httpServiceMock.checkHealth("http://example.net", true) } returns Ok(true)

      val route = api.routing()
      val response = route(Request(method, "/services/health"))

      response.status shouldBe INTERNAL_SERVER_ERROR

      verify(exactly = 1) {
        userConfigMock.services
        httpServiceMock.checkHealth("http://example.org", true)
        httpServiceMock.checkHealth("http://example.net", true)
        httpServiceMock.checkHealth("http://example.com", true)
      }
    }

    @Test
    fun `GET success`() {
      success(GET)
    }

    @Test
    fun `HEAD success`() {
      success(HEAD)
    }

    private fun success(method: Method) {
      val successServiceConfig = HttpServiceConfig(true, "http://example.org")
      every { userConfigMock.services } returns mapOf(
        "successService" to successServiceConfig,
      )

      every { httpServiceMock.checkHealth("http://example.org", true) } returns Ok(true)

      val route = api.routing()
      val response = route(Request(method, "/services/health"))

      response.status shouldBe OK

      verify(exactly = 1) {
        userConfigMock.services
        httpServiceMock.checkHealth("http://example.org", true)
      }
    }

    @Test
    fun `GET unavailable`() {
      unavailable(GET)
    }

    @Test
    fun `HEAD unavailable`() {
      unavailable(HEAD)
    }

    private fun unavailable(method: Method) {
      val unavailableServiceConfig = HttpServiceConfig(true, "http://example.org")
      every { userConfigMock.services } returns mapOf(
        "unavailableService" to unavailableServiceConfig,
      )

      every { httpServiceMock.checkHealth("http://example.org", true) } returns Ok(false)

      val route = api.routing()
      val response = route(Request(method, "/services/health"))

      response.status shouldBe SERVICE_UNAVAILABLE

      verify(exactly = 1) {
        userConfigMock.services
        httpServiceMock.checkHealth("http://example.org", true)
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
      val disabledServiceConfig = HttpServiceConfig(false, "http://example.org")
      every { userConfigMock.services } returns mapOf(
        "disabledService" to disabledServiceConfig,
      )

      val route = api.routing()
      val response = route(Request(method, "/services/health"))

      response.status shouldBe OK

      verify(exactly = 1) {
        userConfigMock.services
      }
    }
  }
}
