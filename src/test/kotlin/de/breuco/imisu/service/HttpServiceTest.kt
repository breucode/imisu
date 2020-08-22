package de.breuco.imisu.service

import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.mockk.clearAllMocks
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class HttpServiceTest {
  private val httpClientMock = mockk<HttpHandler>()

  private lateinit var underTest: HttpService

  @BeforeEach
  fun beforeEach() {
    underTest = HttpService(httpClientMock)
  }

  @AfterEach
  fun afterEach() {
    confirmVerified(httpClientMock)
    clearAllMocks()
  }

  @Test
  fun `HTTP query successful`() {
    val hostName = "http://example.org"

    val responseMock = mockk<Response>()

    every { httpClientMock(Request(Method.OPTIONS, hostName)) } returns responseMock
    every { responseMock.status } returns Status.OK
    every { responseMock.status.successful } returns true

    val result = underTest.checkHealth(hostName)

    result.shouldBeRight(true)

    verify(exactly = 1) {
      httpClientMock(Request(Method.OPTIONS, hostName))
    }
  }

  @Test
  fun `HTTP query unsuccessful`() {
    val hostName = "http://example.org"

    val responseMock = mockk<Response>()

    every { httpClientMock(Request(Method.OPTIONS, hostName)) } returns responseMock
    every { responseMock.status } returns Status.OK
    every { responseMock.status.successful } returns false

    val result = underTest.checkHealth(hostName)

    result.shouldBeRight(false)

    verify(exactly = 1) {
      httpClientMock(Request(Method.OPTIONS, hostName))
    }
  }

  @Test
  fun `Error during HTTP query`() {
    val hostName = "http://example.org"

    every { httpClientMock(Request(Method.OPTIONS, hostName)) } throws Exception()

    val result = underTest.checkHealth(hostName)

    result.shouldBeLeft()

    verify(exactly = 1) {
      httpClientMock(Request(Method.OPTIONS, hostName))
    }
  }

  @Test
  fun `HTTP OPTIONS call not allowed`() {
    val hostName = "http://example.org"

    every { httpClientMock(Request(Method.OPTIONS, hostName)).status } returns Status.METHOD_NOT_ALLOWED
    val responseMock = mockk<Response>()
    every { httpClientMock(Request(Method.GET, hostName)) } returns responseMock
    every { responseMock.status.successful } returns true

    val result = underTest.checkHealth(hostName)

    result.shouldBeRight(true)

    verify(exactly = 1) {
      httpClientMock(Request(Method.OPTIONS, hostName))
      httpClientMock(Request(Method.GET, hostName))
    }
  }
}
