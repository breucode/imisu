package de.breuco.imisu.config

import kotlinx.serialization.Serializable

@Serializable
data class UserConfig(
  val exposeFullApi: Boolean = false,
  val serverPort: Int = 8080,
  val healthChecks: HealthCheckConfig
)
