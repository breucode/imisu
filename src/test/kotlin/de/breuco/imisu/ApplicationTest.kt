package de.breuco.imisu

import de.breuco.imisu.api.Api
import de.breuco.imisu.config.ApplicationConfig
import io.mockk.clearAllMocks
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import mu.KLogger
import org.http4k.server.asServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class ApplicationTest {

  private val apiMock = mockk<Api>()
  private val appConfigMock = mockk<ApplicationConfig>()
  private val loggerMock = mockk<KLogger>(relaxed = true)

  private lateinit var underTest: Application

  @BeforeEach
  fun beforeEach() {
    startKoin {
      modules(
        module {
          single { apiMock }
          single { appConfigMock }
          single { loggerMock }
        }
      )
    }

    underTest = Application()
  }

  @AfterEach
  fun afterEach() {
    stopKoin()
    confirmVerified(apiMock)
    clearAllMocks()
  }

  @Test
  fun `normal start`() {
    every { appConfigMock.userConfig.serverPort } returns 8080
    every { appConfigMock.userConfig.exposeFullApi } returns false

    mockkStatic("org.http4k.server.Http4kServerKt")

    every { apiMock.routing().asServer(any()).start() } returns mockk()

    underTest.run()

    verify(exactly = 1) {
      apiMock.routing().asServer(any()).start()
    }
  }

  @Test
  fun `log warning when full api is exposed`() {
    every { appConfigMock.userConfig.serverPort } returns 8080
    every { appConfigMock.userConfig.exposeFullApi } returns true

    mockkStatic("org.http4k.server.Http4kServerKt")

    every { apiMock.routing().asServer(any()).start() } returns mockk()

    underTest.run()

    verify(exactly = 1) {
      apiMock.routing().asServer(any()).start()
      loggerMock.warn(any<() -> Any?>())
    }
  }
}
