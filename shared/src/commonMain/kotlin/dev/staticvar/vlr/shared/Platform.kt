package dev.staticvar.vlr.shared

/**
 * Platform abstraction for platform-specific implementations.
 */
expect class Platform() {
  val name: String
}
