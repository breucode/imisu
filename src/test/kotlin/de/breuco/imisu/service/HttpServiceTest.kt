package de.breuco.imisu.service

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.unwrap
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.reset
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.matchers.types.shouldBeInstanceOf
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

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

    val responseMock = mock<Response>()

    whenever(httpClientMock(Request(Method.HEAD, hostName)))
      .thenReturn(responseMock)

    doReturn(Status.OK)
      .whenever(responseMock).status

    val result = underTest.checkHealth(hostName, true)

    result.unwrap().shouldBeInstanceOf<HealthCheckSuccess>()

    verify(httpClientMock).invoke(Request(Method.HEAD, hostName))
  }

  @Test
  fun `HTTP query unsuccessful`() {
    val hostName = "http://example.org"

    val responseMock = mock<Response>()

    whenever(httpClientMock(Request(Method.HEAD, hostName)))
      .thenReturn(responseMock)

    whenever(httpClientMock(Request(Method.GET, hostName)))
      .thenReturn(responseMock)

    doReturn(Status.CLIENT_TIMEOUT)
      .whenever(responseMock).status

    val result = underTest.checkHealth(hostName, true)

    result.unwrap().shouldBeInstanceOf<HealthCheckFailure>()

    verify(httpClientMock).invoke(Request(Method.HEAD, hostName))
    verify(httpClientMock).invoke(Request(Method.GET, hostName))
  }

  @Test
  fun `Error during HTTP query`() {
    val hostName = "http://example.org"

    whenever(httpClientMock(Request(Method.HEAD, hostName)))
      .thenAnswer { throw Exception() }

    val result = underTest.checkHealth(hostName, true)

    result.shouldBeInstanceOf<Err<*>>()

    verify(httpClientMock).invoke(Request(Method.HEAD, hostName))
  }

  @Test
  fun `HTTP OPTIONS call not allowed`() {
    val hostName = "http://example.org"

    val notAllowedResponseMock = mock<Response>()
    whenever(httpClientMock(Request(Method.HEAD, hostName)))
      .thenReturn(notAllowedResponseMock)

    doReturn(Status.METHOD_NOT_ALLOWED)
      .whenever(notAllowedResponseMock).status

    val responseMock = mock<Response>()
    whenever(httpClientMock(Request(Method.GET, hostName)))
      .thenReturn(responseMock)

    doReturn(Status.OK)
      .whenever(responseMock).status

    val result = underTest.checkHealth(hostName, true)
    result.unwrap().shouldBeInstanceOf<HealthCheckSuccess>()

    verify(httpClientMock).invoke(Request(Method.HEAD, hostName))
    verify(httpClientMock).invoke(Request(Method.GET, hostName))
  }

  @Test
  fun `Use nonSslValidatingHttpClient when validation is disabled`() {
    val hostName = "http://example.org"

    val responseMock = mock<Response>()

    whenever(nonSslValidatingHttpClientMock(Request(Method.HEAD, hostName)))
      .thenReturn(responseMock)

    doReturn(Status.OK)
      .whenever(responseMock).status

    val result = underTest.checkHealth(hostName, false)

    result.unwrap().shouldBeInstanceOf<HealthCheckSuccess>()

    verify(nonSslValidatingHttpClientMock).invoke(Request(Method.HEAD, hostName))
  }
}
