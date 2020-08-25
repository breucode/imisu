package de.breuco.imisu

import arrow.core.Either
import arrow.core.left
import arrow.core.nonFatalOrThrow
import arrow.core.right

fun <A> Either.Companion.unsafeCatch(f: () -> A) =
  try { f().right() } catch (t: Throwable) { t.nonFatalOrThrow().left() }
