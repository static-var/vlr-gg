/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.event.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import dev.staticvar.designsystem.component.button.PrismButton
import dev.staticvar.designsystem.component.button.PrismButtonStyle
import dev.staticvar.designsystem.component.icon.PrismIconSize
import dev.staticvar.designsystem.component.icon.PrismIconStyle
import dev.staticvar.designsystem.component.icon.PrismIconTint
import dev.staticvar.designsystem.component.tag.PrismTag
import dev.staticvar.designsystem.component.ticket.PrismTicket
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.EventDetails
import dev.staticvar.vlr.domain.model.EventPreview
import dev.staticvar.vlr.sharedui.component.common.FavoriteTicketCardBox
import dev.staticvar.vlr.sharedui.component.common.SharedNetworkIcon
import dev.staticvar.vlr.sharedui.component.event.EventSharedContent
import dev.staticvar.vlr.sharedui.component.event.eventLogoSharedElement
import dev.staticvar.vlr.sharedui.component.event.eventSharedBounds

/**
 * Event detail ticket for title, status, logo, core metadata, and optional actions.
 */
@Composable
public fun EventDetailHeaderItem(
  event: EventDetails,
  modifier: Modifier = Modifier,
  onOpenEvent: (() -> Unit)? = null,
  favoriteAction: (@Composable () -> Unit)? = null,
) {
  EventDetailHeaderContent(
    event = EventPreview(
      id = event.id,
      title = event.title,
      status = event.status,
      prize = event.prize,
      dates = event.dates,
      region = event.region,
      logoUrl = event.logoUrl,
      isFavorite = event.isFavorite,
    ),
    subtitle = event.subtitle,
    teams = event.teams.size.takeIf { it > 0 }?.toString() ?: "TBD",
    modifier = modifier,
    onOpenEvent = onOpenEvent,
    favoriteAction = favoriteAction,
  )
}

/**
 * Immediate event identity shown while the full detail record is loading.
 */
@Composable
public fun EventDetailPreviewHeaderItem(
  event: EventPreview,
  modifier: Modifier = Modifier,
) {
  EventDetailHeaderContent(event = event, modifier = modifier)
}

@Composable
private fun EventDetailHeaderContent(
  event: EventPreview,
  modifier: Modifier = Modifier,
  subtitle: String = "",
  teams: String = "TBD",
  onOpenEvent: (() -> Unit)? = null,
  favoriteAction: (@Composable () -> Unit)? = null,
) {
  FavoriteTicketCardBox(
    selected = event.isFavorite,
    modifier = modifier.fillMaxWidth(),
    favoriteModifier = Modifier.eventSharedBounds(event.id, EventSharedContent.Favorite),
  ) {
    PrismTicket(
      modifier = Modifier.fillMaxWidth().eventSharedBounds(event.id, EventSharedContent.Card),
      header = {
        Row(
          Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
        ) {
          Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs)) {
            Text(
              event.region.ifBlank { "Event" }.uppercase(),
              style = Prism.typography.bodySmall,
              color = Prism.color.contentSecondary,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = event.prize.eventHeroPrizeStat().ifBlank { "TBD" },
              modifier = Modifier.eventSharedBounds(event.id, EventSharedContent.Prize),
              style = Prism.typography.bodySmall,
              color = Prism.color.contentPrimary,
            )
          }
          PrismTag(
            text = event.status.eventDetailLabel,
            modifier = Modifier.eventSharedBounds(event.id, EventSharedContent.Status),
            style = event.status.eventDetailTagStyle,
          )
        }
      },
      stub = {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM)) {
          Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs)) {
            Text("DATES", style = Prism.typography.overline, color = Prism.color.contentSecondary)
            Text(
              text = event.dates.ifBlank { "Dates to be announced" },
              modifier = Modifier.eventSharedBounds(event.id, EventSharedContent.Dates),
              style = Prism.typography.bodySmall,
            )
          }
          Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs)) {
            Text("TEAMS", style = Prism.typography.overline, color = Prism.color.contentSecondary)
            Text(teams, style = Prism.typography.bodySmall)
          }
        }
        if (favoriteAction != null) {
          Column(Modifier.fillMaxWidth().padding(top = Prism.dimens.spacingM)) { favoriteAction() }
        }
        if (onOpenEvent != null) {
          PrismButton(
            onClick = onOpenEvent,
            modifier = Modifier.fillMaxWidth().padding(top = Prism.dimens.spacingM),
            style = PrismButtonStyle.Primary,
          ) {
            Text("View at VLR")
          }
        }
      },
    ) {
      Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
      ) {
        SharedNetworkIcon(
          imageUrl = event.logoUrl,
          contentDescription = event.title,
          imageModifier = Modifier.eventLogoSharedElement(eventId = event.id),
          size = PrismIconSize.Hero,
          style = PrismIconStyle.Plain,
          tint = PrismIconTint.None,
        )
        Text(
          text = event.title,
          modifier = Modifier.eventSharedBounds(event.id, EventSharedContent.Title),
          style = Prism.typography.sectionTitle,
          color = Prism.color.titleColor,
          textAlign = TextAlign.Center,
          maxLines = 3,
          overflow = TextOverflow.Ellipsis,
        )
        if (subtitle.isNotBlank()) {
          Text(
            text = subtitle,
            style = Prism.typography.bodySmall,
            color = Prism.color.labelColor,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}
