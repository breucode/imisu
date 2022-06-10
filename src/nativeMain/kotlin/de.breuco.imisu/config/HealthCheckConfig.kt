package de.breuco.imisu.config

import kotlinx.serialization.Serializable

@Serializable
data class HealthCheckConfig(
  val httpHealthChecks: List<HttpHealthCheckConfig> = emptyList(),
  val dnsHealthChecks: List<DnsHealthCheckConfig> = emptyList(),
  val pingHealthChecks: List<PingHealthCheckConfig> = emptyList()
)

sealed class BaseHealthCheckConfig {
  abstract val name: String
  abstract val enabled: Boolean
  abstract val target: String
}

@Serializable
data class HttpHealthCheckConfig(
  override val name: String,
  override val enabled: Boolean,
  override val target: String,
  val validateSsl: Boolean = true
) : BaseHealthCheckConfig()

@Serializable
data class DnsHealthCheckConfig(
  override val name: String,
  override val enabled: Boolean,
  override val target: String,
  val targetPort: Int = 53,
  val dnsDomain: String = "example.org"
) : BaseHealthCheckConfig()

@Serializable
data class PingHealthCheckConfig(
  override val name: String,
  override val enabled: Boolean,
  override val target: String,
  val timeout: Int = 1000
) : BaseHealthCheckConfig()
