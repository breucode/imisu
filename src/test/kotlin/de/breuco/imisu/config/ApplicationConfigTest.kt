package de.breuco.imisu.config

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import java.nio.file.Paths
import mu.KLogger
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ApplicationConfigTest {

  private val loggerMock = mock<KLogger>(lenient = true)

  @AfterEach
  fun afterEach() {
    reset(loggerMock)
  }

  @Test
  fun `Load config`() {
    val userConfig =
      ApplicationConfig(loggerMock, Paths.get(javaClass.getResource("/complete.conf").toURI()))
        .userConfig

    userConfig.exposeFullApi shouldBe true
    userConfig.exposeSwagger shouldBe true
    userConfig.serverPort shouldBe 9090

    userConfig.services shouldHaveSize 3

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
    httpConfig.validateSsl shouldBe false
    httpConfig.httpEndpoint shouldBe "https://example.org"

    val pingConfig = userConfig.services["examplePing"]
    pingConfig.shouldNotBeNull()
    pingConfig.shouldBeTypeOf<PingServiceConfig>()
    pingConfig.enabled shouldBe false
    pingConfig.pingServer shouldBe "1.1.1.1"
    pingConfig.timeout shouldBe 1337
  }

  @Test
  fun `Load config defaults`() {
    val userConfig =
      ApplicationConfig(loggerMock, Paths.get(javaClass.getResource("/basic.conf").toURI()))
        .userConfig

    userConfig.exposeFullApi shouldBe false
    userConfig.exposeSwagger shouldBe false
    userConfig.serverPort shouldBe 8080

    userConfig.services shouldHaveSize 3

    val dnsConfig = userConfig.services["dnsTest"]
    dnsConfig.shouldNotBeNull()
    dnsConfig.shouldBeTypeOf<DnsServiceConfig>()
    dnsConfig.dnsDomain shouldBe "example.org"
    dnsConfig.dnsServerPort shouldBe 53

    val httpConfig = userConfig.services["httpTest"]
    httpConfig.shouldNotBeNull()
    httpConfig.shouldBeTypeOf<HttpServiceConfig>()
    httpConfig.httpEndpoint shouldBe "https://example.org"
    httpConfig.validateSsl shouldBe true

    val pingConfig = userConfig.services["pingTest"]
    pingConfig.shouldNotBeNull()
    pingConfig.shouldBeTypeOf<PingServiceConfig>()
    pingConfig.enabled shouldBe true
    pingConfig.pingServer shouldBe "1.1.1.1"
    pingConfig.timeout shouldBe 1000
  }

  @Test
  fun `Exit application, when forbidden service names are used`() {
    Mockito.mockStatic(Runtime::class.java).use {
      val runtimeMock = mock<Runtime>()
      it.`when`<Any> { Runtime.getRuntime() }.thenReturn(runtimeMock)

      whenever(Runtime.getRuntime()).thenReturn(runtimeMock)

      shouldThrow<Exception> {
        ApplicationConfig(
            loggerMock,
            Paths.get(javaClass.getResource("/forbidden-service-name.conf").toURI())
          )
          .userConfig
      }

      verify(loggerMock).error(any<() -> Any?>())
      verify(runtimeMock).exit(ArgumentMatchers.intThat { exitCode -> exitCode != 0 })
    }
  }

  @Test
  fun `Exit application on broken config`() {
    Mockito.mockStatic(Runtime::class.java).use {
      val runtimeMock = mock<Runtime>()
      it.`when`<Any> { Runtime.getRuntime() }.thenReturn(runtimeMock)

      whenever(Runtime.getRuntime()).thenReturn(runtimeMock)

      shouldThrow<Exception> {
        ApplicationConfig(loggerMock, Paths.get(javaClass.getResource("/broken.conf").toURI()))
          .userConfig
      }

      verify(loggerMock).error(any<() -> Any?>())
      verify(runtimeMock).exit(ArgumentMatchers.intThat { exitCode -> exitCode != 0 })
    }
  }
}
