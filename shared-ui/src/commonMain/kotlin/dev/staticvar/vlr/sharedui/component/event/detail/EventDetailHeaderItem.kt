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
import dev.staticvar.designsystem.component.ticket.PrismTicketStyle
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.EventDetails
import dev.staticvar.vlr.domain.model.EventPreview
import dev.staticvar.vlr.sharedui.component.common.FavoriteTicketCardBox
import dev.staticvar.vlr.sharedui.component.common.SharedNetworkIcon
import dev.staticvar.vlr.sharedui.component.common.currentTransitionContentFade
import dev.staticvar.vlr.sharedui.component.common.transitionContentFade
import dev.staticvar.vlr.sharedui.component.event.EventFavoriteReasons
import dev.staticvar.vlr.sharedui.component.event.EventSharedContent
import dev.staticvar.vlr.sharedui.component.event.eventLogoSharedElement
import dev.staticvar.vlr.sharedui.component.event.eventSharedBounds
import org.jetbrains.compose.resources.stringResource
import vlr.shared_ui.generated.resources.Res
import vlr.shared_ui.generated.resources.match_event_dates
import vlr.shared_ui.generated.resources.match_event_dates_tba
import vlr.shared_ui.generated.resources.match_event_event
import vlr.shared_ui.generated.resources.match_event_tbd
import vlr.shared_ui.generated.resources.match_event_teams
import vlr.shared_ui.generated.resources.match_event_view_at_vlr

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
      favoriteReasons = event.favoriteReasons,
    ),
    subtitle = event.subtitle,
    teams = event.teams.size.takeIf { it > 0 }?.toString() ?: stringResource(Res.string.match_event_tbd),
    modifier = modifier,
    onOpenEvent = onOpenEvent,
    favoriteAction = favoriteAction,
  )
}

/**
 * Immediate event identity shown while the full detail record is loading.
 */
@Composable
public fun EventDetailPreviewHeaderItem(event: EventPreview, modifier: Modifier = Modifier) {
  EventDetailHeaderContent(event = event, modifier = modifier)
}

@Composable
private fun EventDetailHeaderContent(
  event: EventPreview,
  modifier: Modifier = Modifier,
  subtitle: String = "",
  teams: String = stringResource(Res.string.match_event_tbd),
  onOpenEvent: (() -> Unit)? = null,
  favoriteAction: (@Composable () -> Unit)? = null,
) {
  val extraContentFade = currentTransitionContentFade()
  FavoriteTicketCardBox(
    selected = event.isFavorite,
    modifier = modifier.fillMaxWidth(),
    favoriteModifier = Modifier.transitionContentFade(extraContentFade),
  ) {
    PrismTicket(
      modifier = Modifier.fillMaxWidth().eventSharedBounds(event.id, EventSharedContent.Card),
      animateSizeChanges = extraContentFade.isSettled,
      header = {
        Column(
          modifier = Modifier.transitionContentFade(extraContentFade),
          verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs),
        ) {
          Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
          ) {
            Text(
              event.region.ifBlank { stringResource(Res.string.match_event_event) }.uppercase(),
              modifier = Modifier.weight(1f),
              style = Prism.typography.bodySmall,
              color = Prism.color.contentSecondary,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )
            PrismTag(
              text = event.status.eventDetailLabel,
              style = event.status.eventDetailTagStyle,
            )
          }
          Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
          ) {
            Text(
              text = event.prize.eventHeroPrizeStat().ifBlank { stringResource(Res.string.match_event_tbd) },
              modifier = Modifier.weight(1f),
              style = Prism.typography.bodySmall,
              color = Prism.color.contentPrimary,
            )
            if (event.favoriteReasons.isNotEmpty()) {
              EventFavoriteReasons(event.favoriteReasons, modifier = Modifier.weight(1f))
            }
          }
        }
      },
      stub = {
        Column(Modifier.fillMaxWidth().transitionContentFade(extraContentFade)) {
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs)) {
              Text(
                stringResource(Res.string.match_event_dates),
                style = Prism.typography.overline,
                color = Prism.color.contentSecondary,
              )
              Text(
                text = event.dates.ifBlank { stringResource(Res.string.match_event_dates_tba) },
                style = Prism.typography.bodySmall,
              )
            }
            Column(
              horizontalAlignment = Alignment.End,
              verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs),
            ) {
              Text(
                stringResource(Res.string.match_event_teams),
                style = Prism.typography.overline,
                color = Prism.color.contentSecondary,
              )
              Text(teams, style = Prism.typography.bodySmall)
            }
          }
          favoriteAction?.let { action ->
            Column(Modifier.fillMaxWidth().padding(top = Prism.dimens.spacingM)) { action() }
          }
          onOpenEvent?.let { openEvent ->
            PrismButton(
              onClick = openEvent,
              modifier = Modifier.fillMaxWidth().padding(top = Prism.dimens.spacingM),
              style = PrismButtonStyle.Primary,
            ) {
              Text(stringResource(Res.string.match_event_view_at_vlr))
            }
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
          parentBackground = PrismTicketStyle.Standard.containerColor,
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
            modifier = Modifier.transitionContentFade(extraContentFade),
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
