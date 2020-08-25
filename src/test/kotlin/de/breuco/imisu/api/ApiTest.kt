package de.breuco.imisu.api

import de.breuco.imisu.api.routes.Services
import de.breuco.imisu.config.ApplicationConfig
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
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.kotest.shouldHaveBody
import org.http4k.kotest.shouldNotHaveBody
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ApiTest {
  private val appConfigMock = mockk<ApplicationConfig>()
  private val userConfigMock = mockk<UserConfig>()
  private val dnsServiceMock = mockk<DnsService>()
  private val httpServiceMock = mockk<HttpService>()
  private val pingServiceMock = mockk<PingService>()

  private lateinit var underTest: Api

  @BeforeEach
  fun beforeEach() {
    every { appConfigMock.userConfig } returns userConfigMock
    every { appConfigMock.versions } returns Versions("appVersion", "swaggerUiVersion")
    underTest = Api(appConfigMock, Services(appConfigMock, dnsServiceMock, httpServiceMock, pingServiceMock))
  }

  @AfterEach
  fun afterEach() {
    confirmVerified(dnsServiceMock, httpServiceMock, pingServiceMock, userConfigMock)
    clearAllMocks()
  }

  @Test
  fun `404 for endpoints, which should be disabled when exposeFullApi == false`() {
    every { userConfigMock.exposeFullApi } returns false
    every { userConfigMock.exposeSwagger } returns false
    val disabledRoutes = listOf(
      "/services",
      "/services/testId"
    )

    val routing = underTest.routing()

    val responses = disabledRoutes.map {
      routing(Request(Method.GET, it))
    }

    responses.forEach { it.status shouldBe Status.NOT_FOUND }

    verify(exactly = 1) {
      userConfigMock.exposeFullApi
      userConfigMock.exposeSwagger
    }
  }

  @Test
  fun `Don't provide swagger json when disabled`() {
    every { userConfigMock.exposeFullApi } returns false
    every { userConfigMock.exposeSwagger } returns false

    val routing = underTest.routing()

    val response = routing(Request(Method.GET, "/swagger.json"))

    response.status shouldBe Status.OK
    response shouldHaveBody ""

    verify(exactly = 1) {
      userConfigMock.exposeFullApi
      userConfigMock.exposeSwagger
    }
  }

  @Test
  fun `Provide swagger json when enabled`() {
    every { userConfigMock.exposeFullApi } returns false
    every { userConfigMock.exposeSwagger } returns true

    val routing = underTest.routing()

    val response = routing(Request(Method.GET, "/swagger.json"))

    response.status shouldBe Status.OK
    response shouldNotHaveBody ""

    verify(exactly = 1) {
      userConfigMock.exposeFullApi
      userConfigMock.exposeSwagger
    }
  }
}
