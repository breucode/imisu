package de.breuco.imisu.service

import io.kotest.matchers.result.shouldBeFailure
import io.kotest.matchers.types.shouldBeInstanceOf
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Answers
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

internal class HttpServiceTest {
  private val httpClientMock = mock<HttpHandler>()
  private val nonSslValidatingHttpClientMock = mock<HttpHandler>()

  private lateinit var underTest: HttpService

  @BeforeEach
  fun beforeEach() {
    underTest = HttpService(
      httpClientMock,
      nonSslValidatingHttpClientMock
    )
  }

  @AfterEach
  fun afterEach() {
    verifyNoMoreInteractions(httpClientMock, nonSslValidatingHttpClientMock)
    reset(httpClientMock, nonSslValidatingHttpClientMock)
  }

  @Test
  fun `HTTP query successful`() {
    val hostName = "http://example.org"

    val responseMock = mock<Response>(defaultAnswer = Answers.RETURNS_MOCKS)

    whenever(httpClientMock(Request(Method.HEAD, hostName))).thenReturn(responseMock)
    whenever(responseMock.status).thenReturn(Status.OK)

    val result = underTest.checkHealth(hostName, true)

    result.getOrThrow().shouldBeInstanceOf<HealthCheckSuccess>()

    verify(httpClientMock).invoke(Request(Method.HEAD, hostName))
  }

  @Test
  fun `HTTP query unsuccessful`() {
    val hostName = "http://example.org"

    val responseMock = mockk<Response>()

    every { httpClientMock(Request(Method.HEAD, hostName)) } returns responseMock
    every { httpClientMock(Request(Method.GET, hostName)) } returns responseMock
    every { responseMock.status } returns Status.OK
    every { responseMock.status.successful } returns false

    val result = underTest.checkHealth(hostName, true)

    result.getOrThrow().shouldBeInstanceOf<HealthCheckFailure>()

    verify(exactly = 1) {
      httpClientMock(Request(Method.HEAD, hostName))
      httpClientMock(Request(Method.GET, hostName))
    }
  }

  /*
  @Test
  fun `Error during HTTP query`() {
    val hostName = "http://example.org"

    every { httpClientMock(Request(Method.HEAD, hostName)) } throws Exception()

    val result = underTest.checkHealth(hostName, true)

    result.shouldBeFailure()

    verify(exactly = 1) {
      httpClientMock(Request(Method.HEAD, hostName))
    }
  }

  @Test
  fun `HTTP OPTIONS call not allowed`() {
    val hostName = "http://example.org"

    every { httpClientMock(Request(Method.HEAD, hostName)).status } returns Status.METHOD_NOT_ALLOWED
    val responseMock = mockk<Response>()
    every { httpClientMock(Request(Method.GET, hostName)) } returns responseMock
    every { responseMock.status.successful } returns true

    val result = underTest.checkHealth(hostName, true)

    result.getOrThrow().shouldBeInstanceOf<HealthCheckSuccess>()

    verify(exactly = 1) {
      httpClientMock(Request(Method.HEAD, hostName))
      httpClientMock(Request(Method.GET, hostName))
    }
  }

  @Test
  fun `Use nonSslValidatingHttpClient when validation is disabled`() {
    val hostName = "http://example.org"

    every { nonSslValidatingHttpClientMock(Request(Method.HEAD, hostName)).status } returns Status.METHOD_NOT_ALLOWED
    val responseMock = mockk<Response>()
    every { nonSslValidatingHttpClientMock(Request(Method.GET, hostName)) } returns responseMock
    every { responseMock.status.successful } returns true

    val result = underTest.checkHealth(hostName, false)

    result.getOrThrow().shouldBeInstanceOf<HealthCheckSuccess>()

    verify(exactly = 1) {
      nonSslValidatingHttpClientMock(Request(Method.HEAD, hostName))
      nonSslValidatingHttpClientMock(Request(Method.GET, hostName))
    }
  }*/
}
