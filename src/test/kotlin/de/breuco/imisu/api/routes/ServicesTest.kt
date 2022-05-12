package de.breuco.imisu.api.routes

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
import io.kotest.matchers.shouldBe
import javax.net.ssl.SSLException
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
import org.mockito.Answers
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

class ServicesTest {
  private val appConfigMock = mock<ApplicationConfig>()
  private val userConfigMock = mock<UserConfig>(defaultAnswer = Answers.RETURNS_DEEP_STUBS)
  private val dnsServiceMock = mock<DnsService>()
  private val httpServiceMock = mock<HttpService>()
  private val pingServiceMock = mock<PingService>()

  private lateinit var api: Api

  @BeforeEach
  fun beforeEach() {
    whenever(appConfigMock.userConfig).thenReturn(userConfigMock)
    whenever(userConfigMock.exposeFullApi).thenReturn(true)
    whenever(userConfigMock.exposeSwagger).thenReturn(false)
    whenever(appConfigMock.versions).thenReturn(Versions("appVersion", "swaggerUiVersion"))
    api =
      Api(appConfigMock, Services(appConfigMock, dnsServiceMock, httpServiceMock, pingServiceMock))
  }

  @AfterEach
  fun afterEach() {
    verify(userConfigMock).exposeFullApi
    verify(userConfigMock).exposeSwagger
    verifyNoMoreInteractions(dnsServiceMock, httpServiceMock, pingServiceMock)

    reset(
      appConfigMock,
      userConfigMock,
      dnsServiceMock,
      httpServiceMock,
      pingServiceMock,
    )
  }

  @Test
  fun `GET one service`() {
    val serviceId = "testServiceId"
    val httpEndpoint = "http://example.org"

    whenever(userConfigMock.services)
      .thenReturn(mapOf(serviceId to HttpServiceConfig(true, httpEndpoint)))

    val route = api.routing()
    val response = route(Request(GET, "/services"))

    response.status shouldBe OK
    response shouldHaveBody
      """{"$serviceId":{"enabled":true,"httpEndpoint":"$httpEndpoint","validateSsl":true}}"""

    verify(userConfigMock).services
  }

  @Test
  fun `GET service disabled`() {
    val serviceId = "testServiceId"
    val httpEndpoint = "http://example.org"

    whenever(userConfigMock.services)
      .thenReturn(mapOf(serviceId to HttpServiceConfig(false, httpEndpoint)))

    val route = api.routing()
    val response = route(Request(GET, "/services"))

    response.status shouldBe OK
    response shouldHaveBody "{}"

    verify(userConfigMock).services
  }

  @Nested
  inner class Id {
    @Test
    fun `GET ServiceConfig found`() {
      val serviceId = "testServiceId"
      val httpEndpoint = "http://example.org"

      whenever(userConfigMock.services[serviceId]).thenReturn(HttpServiceConfig(true, httpEndpoint))

      val route = api.routing()
      val response = route(Request(GET, "/services/$serviceId"))

      response.status shouldBe OK
      response shouldHaveBody
        """{"enabled":true,"httpEndpoint":"$httpEndpoint","validateSsl":true}"""
    }

    @Test
    fun `GET ServiceConfig disabled`() {
      val serviceId = "testServiceId"
      val httpEndpoint = "http://example.org"

      whenever(userConfigMock.services[serviceId])
        .thenReturn(HttpServiceConfig(false, httpEndpoint))

      val route = api.routing()
      val response = route(Request(GET, "/services/$serviceId"))

      response.status shouldBe NOT_FOUND
    }

    @Test
    fun `GET ServiceConfig not found`() {
      val serviceId = "testServiceId"

      whenever(userConfigMock.services[serviceId]).thenReturn(null)

      val route = api.routing()
      val response = route(Request(GET, "/services/$serviceId"))

      response.status shouldBe NOT_FOUND
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

        whenever(userConfigMock.services[serviceId])
          .thenReturn(HttpServiceConfig(true, httpEndpoint))
        whenever(httpServiceMock.checkHealth(httpEndpoint, true))
          .thenReturn(Result.success(HealthCheckSuccess))

        val route = api.routing()
        val response = route(Request(method, "/services/$serviceId/health"))

        response.status shouldBe OK

        verify(httpServiceMock).checkHealth(httpEndpoint, true)
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

        whenever(userConfigMock.services[serviceId])
          .thenReturn(HttpServiceConfig(false, httpEndpoint))

        val route = api.routing()
        val response = route(Request(method, "/services/$serviceId/health"))

        response.status shouldBe NOT_FOUND
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

        whenever(userConfigMock.services[serviceId]).thenReturn(DnsServiceConfig(true, dnsServer))
        whenever(dnsServiceMock.checkHealth("example.org", dnsServer, 53))
          .thenReturn(Result.success(HealthCheckSuccess))

        val route = api.routing()
        val response = route(Request(method, "/services/$serviceId/health"))

        response.status shouldBe OK

        verify(dnsServiceMock).checkHealth("example.org", dnsServer, 53)
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

        whenever(userConfigMock.services[serviceId]).thenReturn(PingServiceConfig(true, pingServer))
        whenever(pingServiceMock.checkHealth(pingServer, 1000))
          .thenReturn(Result.success(HealthCheckSuccess))

        val route = api.routing()
        val response = route(Request(method, "/services/$serviceId/health"))

        response.status shouldBe OK

        verify(pingServiceMock).checkHealth(pingServer, 1000)
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

        whenever(userConfigMock.services[serviceId])
          .thenReturn(HttpServiceConfig(true, httpEndpoint))
        whenever(httpServiceMock.checkHealth(httpEndpoint, true))
          .thenReturn(Result.success(HealthCheckFailure()))

        val route = api.routing()
        val response = route(Request(method, "/services/$serviceId/health"))

        response.status shouldBe SERVER_IS_DOWN
        verify(httpServiceMock).checkHealth(httpEndpoint, true)
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

        whenever(userConfigMock.services[serviceId])
          .thenReturn(HttpServiceConfig(true, httpEndpoint))
        whenever(httpServiceMock.checkHealth(httpEndpoint, true))
          .thenReturn(Result.failure(Exception()))

        val route = api.routing()
        val response = route(Request(method, "/services/$serviceId/health"))

        response.status shouldBe INTERNAL_SERVER_ERROR

        verify(httpServiceMock).checkHealth(httpEndpoint, true)
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

        whenever(userConfigMock.services[serviceId])
          .thenReturn(HttpServiceConfig(true, httpEndpoint))
        whenever(httpServiceMock.checkHealth(httpEndpoint, true))
          .thenReturn(Result.success(HealthCheckFailure(SSLException(""))))

        val route = api.routing()
        val response = route(Request(method, "/services/$serviceId/health"))

        response.status shouldBe SSL_HANDSHAKE_FAILED

        verify(httpServiceMock).checkHealth(httpEndpoint, true)
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

        whenever(userConfigMock.services[serviceId]).thenReturn(null)

        val route = api.routing()
        val response = route(Request(method, "/services/$serviceId/health"))

        response.status shouldBe NOT_FOUND
      }
    }
  }
}
