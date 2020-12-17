package de.breuco.imisu.service

sealed class HealthCheckResult

object HealthCheckSuccess : HealthCheckResult()

data class HealthCheckFailure(
  val cause: Throwable? = null
) : HealthCheckResult()

fun Boolean.toHealthCheckResult(): HealthCheckResult =
  if (this) {
    HealthCheckSuccess
  } else {
    HealthCheckFailure()
  }
