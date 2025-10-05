package dev.staticvar.vlr.remotesource

import kotlin.io.use
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.io.readString

internal actual fun readFixture(fileName: String): String {
  val loader = requireNotNull(Thread.currentThread().contextClassLoader) {
    "Context class loader unavailable while loading fixture $fileName"
  }
  val resource = requireNotNull(loader.getResource(fileName)) {
    "Fixture $fileName was not found in test resources."
  }
  return resource.openStream().use { stream ->
    stream.asSource().buffered().use { it.readString() }
  }
}
