package de.breuco.imisu

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.fold

fun <V, E> Result<V, E>.isSuccess(): Boolean =
  this.fold({ true }, { false })

fun <V, E> Result<V, E>.isError(): Boolean =
  !this.isSuccess()
