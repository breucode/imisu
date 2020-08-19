package de.breuco.imisu

import arrow.core.Either
import kotlinx.coroutines.runBlocking

fun <R> Either.Companion.unsafeCatch(f: suspend () -> R) =
  runBlocking { catch(f) }
