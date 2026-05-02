package dev.staticvar.vlr.featureevents.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import dev.staticvar.designsystem.preview.PrismPreview
import dev.staticvar.designsystem.preview.PrismPreviewProvider
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.designsystem.prism.PrismTheme
import dev.staticvar.designsystem.prism.PrismVariant
import dev.staticvar.vlr.domain.model.EventPreview
import dev.staticvar.vlr.domain.model.EventStatus

@PrismPreview
@Composable
internal fun EventsOverviewPreview(
  @PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant,
) {
  PrismTheme(variant = variant) {
    EventsOverviewScreen(
      uiState =
        EventsUiState(
          events =
            listOf(
              sampleEventPreview(id = "event-1", status = EventStatus.ONGOING),
              sampleEventPreview(id = "event-2", status = EventStatus.UPCOMING),
            ),
          selectedStatus = EventStatusFilter.Ongoing,
          isLoading = false,
        ),
      onFilterSelected = {},
      onEventSelected = {},
      modifier = Modifier.fillMaxSize().background(Prism.color.background),
    )
  }
}

private fun sampleEventPreview(id: String, status: EventStatus): EventPreview =
  EventPreview(
    id = id,
    title = "Champions Tour $id",
    status = status,
    prize = "$" + "250k",
    dates = "Mar 8 - Mar 16",
    region = "Global",
    logoUrl = "",
  )
