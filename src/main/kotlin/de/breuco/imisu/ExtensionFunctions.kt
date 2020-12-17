package de.breuco.imisu

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result

// Replace with recoverIf
fun <V, E> Result<V, E>.toSuccessIf(predicate: (E) -> Boolean, transform: (E) -> V): Result<V, E> {
  return when (this) {
    is Err -> if (predicate(this.error)) {
      Ok(transform(this.error))
    } else {
      this
    }
    is Ok -> this
  }
}
