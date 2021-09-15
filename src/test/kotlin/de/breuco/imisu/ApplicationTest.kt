package de.breuco.imisu

import de.breuco.imisu.api.Api
import de.breuco.imisu.config.ApplicationConfig
import mu.KLogger
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.stopKoin
import org.mockito.Answers
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ApplicationTest {

  private val appConfigMock = mock<ApplicationConfig>(defaultAnswer = Answers.RETURNS_DEEP_STUBS)
  private val loggerMock = mock<KLogger>(lenient = true)

  private lateinit var underTest: Application

  @BeforeEach
  fun beforeEach() {
    underTest = Application(Api(appConfigMock, mock()), appConfigMock, loggerMock)
  }

  @AfterEach
  fun afterEach() {
    stopKoin()
    reset(appConfigMock, loggerMock)
  }

  @Test
  fun `log warning when full api is exposed`() {
    whenever(appConfigMock.userConfig.serverPort).thenReturn(8081)
    whenever(appConfigMock.userConfig.exposeFullApi).thenReturn(true)

    underTest.run()

    verify(loggerMock).warn(any<() -> Any?>())
  }
}
