package dev.staticvar.vlr.domain.model

/**
 * Domain model for event list item.
 */
data class EventPreview(
  val id: String,
  val title: String,
  val status: EventStatus,
  val prize: String,
  val dates: String,
  val region: String,
  val logoUrl: String,
  val isFavorite: Boolean = false,
)
