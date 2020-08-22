package de.breuco.imisu

import org.junit.jupiter.api.Test
import java.nio.file.Path

class ApplicationContextTest {

  @Test
  fun `Application starts successfully`() {
    val testConfigPath = Path.of(javaClass.getResource("/basic-conf.conf").toURI()).toString()
    CliApplicationStarter().main(arrayOf("--config-path", testConfigPath))
  }
}
