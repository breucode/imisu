package de.breuco.imisu

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import platform.posix.exit
import platform.posix.fclose
import platform.posix.fgets
import platform.posix.fopen

fun readTextFile(filePath: String): String {
  val file = fopen(filePath, "r") ?: return ""
  val returnBuffer = StringBuilder()

  try {
    memScoped {
      val bufferLength = 64 * 1024
      val buffer = allocArray<ByteVar>(bufferLength)
      var line = fgets(buffer, bufferLength, file)?.toKString()

      while (line != null) {
        returnBuffer.append(line)
        line = fgets(buffer, bufferLength, file)?.toKString()
      }
    }

    StringBuilder().toString()
  } finally {
    fclose(file)
  }

  return returnBuffer.toString()
}

fun exitWithMessage(message: String, exitCode: Int) {
  println(message)
  exit(exitCode)
  throw Exception()
}
