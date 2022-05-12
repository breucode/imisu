package de.breuco.imisu.api

import de.breuco.imisu.api.routes.Services
import de.breuco.imisu.config.ApplicationConfig
import de.breuco.imisu.config.UserConfig
import de.breuco.imisu.config.Versions
import de.breuco.imisu.service.DnsService
import de.breuco.imisu.service.HttpService
import de.breuco.imisu.service.PingService
import io.kotest.matchers.shouldBe
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.kotest.shouldHaveBody
import org.http4k.kotest.shouldNotHaveBody
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

class ApiTest {
  private val appConfigMock = mock<ApplicationConfig>()
  private val userConfigMock = mock<UserConfig>()
  private val dnsServiceMock = mock<DnsService>()
  private val httpServiceMock = mock<HttpService>()
  private val pingServiceMock = mock<PingService>()

  private lateinit var underTest: Api

  @BeforeEach
  fun beforeEach() {
    whenever(appConfigMock.userConfig).thenReturn(userConfigMock)
    whenever(appConfigMock.versions).thenReturn(Versions("appVersion", "swaggerUiVersion"))
    underTest =
      Api(appConfigMock, Services(appConfigMock, dnsServiceMock, httpServiceMock, pingServiceMock))
  }

  @AfterEach
  fun afterEach() {
    verifyNoMoreInteractions(dnsServiceMock, httpServiceMock, pingServiceMock, userConfigMock)
    reset(
      appConfigMock,
      userConfigMock,
      dnsServiceMock,
      httpServiceMock,
      pingServiceMock,
    )
  }

  @Test
  fun `404 for endpoints, which should be disabled when exposeFullApi == false`() {
    whenever(userConfigMock.exposeFullApi).thenReturn(false)
    whenever(userConfigMock.exposeSwagger).thenReturn(false)
    val disabledRoutes = listOf("/services", "/services/testId")

    val routing = underTest.routing()

    val responses = disabledRoutes.map { routing(Request(Method.GET, it)) }

    responses.forEach { it.status shouldBe Status.NOT_FOUND }

    verify(userConfigMock).exposeFullApi
    verify(userConfigMock).exposeSwagger
  }

  @Test
  fun `Don't provide swagger json when disabled`() {
    whenever(userConfigMock.exposeFullApi).thenReturn(false)
    whenever(userConfigMock.exposeSwagger).thenReturn(false)

    val routing = underTest.routing()

    val response = routing(Request(Method.GET, "/swagger.json"))

    response.status shouldBe Status.OK
    response shouldHaveBody ""

    verify(userConfigMock).exposeFullApi
    verify(userConfigMock).exposeSwagger
  }

  @Test
  fun `Provide swagger json when enabled`() {
    whenever(userConfigMock.exposeFullApi).thenReturn(false)
    whenever(userConfigMock.exposeSwagger).thenReturn(true)

    val routing = underTest.routing()

    val response = routing(Request(Method.GET, "/swagger.json"))

    response.status shouldBe Status.OK
    response shouldNotHaveBody ""

    verify(userConfigMock).exposeFullApi
    verify(userConfigMock).exposeSwagger
  }
}
