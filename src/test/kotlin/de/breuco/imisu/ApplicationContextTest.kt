package de.breuco.imisu

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.koin.core.context.stopKoin
import java.nio.file.Paths

class ApplicationContextTest {

  @AfterEach
  fun afterEach() {
    stopKoin()
  }

  @Test
  fun `Application starts successfully`() {
    val testConfigPath = Paths.get(javaClass.getResource("/basic.conf").toURI()).toString()
    main(arrayOf("--config-path", testConfigPath))
  }
}
