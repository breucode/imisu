package de.breuco.imisu.config

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import mu.KLogger
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.nio.file.Path

class ApplicationConfigTest {

  private val loggerMock = mockk<KLogger>(relaxed = true)

  @AfterEach
  fun afterEach() {
    clearAllMocks()
  }

  @Test
  fun `Load config`() {
    val userConfig =
      ApplicationConfig(loggerMock, Path.of(javaClass.getResource("/complete.conf").toURI())).userConfig

    userConfig.exposeFullApi shouldBe true
    userConfig.exposeSwagger shouldBe true
    userConfig.serverPort shouldBe 9090

    userConfig.services shouldHaveSize 2

    val dnsConfig = userConfig.services["exampleDns"]
    dnsConfig.shouldNotBeNull()
    dnsConfig.shouldBeTypeOf<DnsServiceConfig>()
    dnsConfig.enabled shouldBe true
    dnsConfig.dnsDomain shouldBe "example.com"
    dnsConfig.dnsServer shouldBe "8.8.8.8"
    dnsConfig.dnsServerPort shouldBe 5353

    val httpConfig = userConfig.services["exampleHttp"]
    httpConfig.shouldNotBeNull()
    httpConfig.shouldBeTypeOf<HttpServiceConfig>()
    httpConfig.enabled shouldBe false
    httpConfig.httpEndpoint shouldBe "https://example.org"
  }

  @Test
  fun `Load config defaults`() {
    val userConfig =
      ApplicationConfig(loggerMock, Path.of(javaClass.getResource("/basic.conf").toURI())).userConfig

    userConfig.exposeFullApi shouldBe false
    userConfig.exposeSwagger shouldBe false
    userConfig.serverPort shouldBe 8080

    userConfig.services shouldHaveSize 1

    val dnsConfig = userConfig.services["dnsTest"]
    dnsConfig.shouldNotBeNull()
    dnsConfig.shouldBeTypeOf<DnsServiceConfig>()
    dnsConfig.dnsDomain shouldBe "example.org"
    dnsConfig.dnsServerPort shouldBe 53
  }

  @Test
  fun `Exit application on broken config`() {
    mockkStatic(Runtime::class)

    val runtimeMock = mockk<Runtime>()
    every { Runtime.getRuntime() } returns runtimeMock
    every { runtimeMock.exit(neq(0)) } just Runs

    shouldThrow<Exception> {
      ApplicationConfig(loggerMock, Path.of(javaClass.getResource("/broken.conf").toURI()))
    }

    verify(exactly = 1) {
      loggerMock.error(any<() -> Any?>())
      runtimeMock.exit(neq(0))
    }
  }
}
