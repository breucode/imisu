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
import java.nio.file.Paths

class ApplicationConfigTest {

  private val loggerMock = mockk<KLogger>(relaxed = true)

  @AfterEach
  fun afterEach() {
    clearAllMocks()
  }

  @Test
  fun `Load config`() {
    val userConfig =
      ApplicationConfig(loggerMock, Paths.get(javaClass.getResource("/complete.conf").toURI())).userConfig

    userConfig.exposeFullApi shouldBe true
    userConfig.exposeSwagger shouldBe true
    userConfig.serverPort shouldBe 9090

    userConfig.services shouldHaveSize 4

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

    val tcpConfig = userConfig.services["exampleTcp"]
    tcpConfig.shouldNotBeNull()
    tcpConfig.shouldBeTypeOf<TcpServiceConfig>()
    tcpConfig.enabled shouldBe true
    tcpConfig.tcpServer shouldBe "towel.blinkenlights.nl"
    tcpConfig.tcpServerPort shouldBe 23
  }

  @Test
  fun `Load config defaults`() {
    val userConfig =
      ApplicationConfig(loggerMock, Paths.get(javaClass.getResource("/basic.conf").toURI())).userConfig

    userConfig.exposeFullApi shouldBe false
    userConfig.exposeSwagger shouldBe false
    userConfig.serverPort shouldBe 8080

    userConfig.services shouldHaveSize 4

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

    val tcpConfig = userConfig.services["tcpTest"]
    tcpConfig.shouldNotBeNull()
    tcpConfig.shouldBeTypeOf<TcpServiceConfig>()
    tcpConfig.enabled shouldBe true
    tcpConfig.tcpServer shouldBe "horizons.jpl.nasa.gov"
    tcpConfig.tcpServerPort shouldBe 6775
  }

  @Test
  fun `Exit application, when forbidden service names are used`() {
    mockkStatic(Runtime::class)

    val runtimeMock = mockk<Runtime>()
    every { Runtime.getRuntime() } returns runtimeMock
    every { runtimeMock.exit(neq(0)) } just Runs

    shouldThrow<Exception> {
      ApplicationConfig(loggerMock, Paths.get(javaClass.getResource("/forbidden-service-name.conf").toURI())).userConfig
    }

    verify(exactly = 1) {
      loggerMock.error(any<() -> Any?>())
      runtimeMock.exit(neq(0))
    }
  }

  @Test
  fun `Exit application on broken config`() {
    mockkStatic(Runtime::class)

    val runtimeMock = mockk<Runtime>()
    every { Runtime.getRuntime() } returns runtimeMock
    every { runtimeMock.exit(neq(0)) } just Runs

    shouldThrow<Exception> {
      ApplicationConfig(loggerMock, Paths.get(javaClass.getResource("/broken.conf").toURI())).userConfig
    }

    verify(exactly = 1) {
      loggerMock.error(any<() -> Any?>())
      runtimeMock.exit(neq(0))
    }
  }
}
