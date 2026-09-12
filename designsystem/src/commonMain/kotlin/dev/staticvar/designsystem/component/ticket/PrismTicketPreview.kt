/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.ticket

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.staticvar.designsystem.component.surface.PrismSurface
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.designsystem.prism.PrismTheme
import dev.staticvar.designsystem.prism.PrismThemeFamily
import dev.staticvar.designsystem.prism.PrismVariant

@Preview(name = "Brutalist Light", group = "Prism/Ticket")
@Composable
internal fun TicketBrutalistLightPreview() = TicketPreview(PrismThemeFamily.Brutalist, PrismVariant.Light)

@Preview(name = "Brutalist Dark", group = "Prism/Ticket")
@Composable
internal fun TicketBrutalistDarkPreview() = TicketPreview(PrismThemeFamily.Brutalist, PrismVariant.Dark)

@Preview(name = "Catppuccin Light", group = "Prism/Ticket")
@Composable
internal fun TicketCatppuccinLightPreview() = TicketPreview(PrismThemeFamily.Catppuccin, PrismVariant.Light)

@Preview(name = "Catppuccin Dark", group = "Prism/Ticket")
@Composable
internal fun TicketCatppuccinDarkPreview() = TicketPreview(PrismThemeFamily.Catppuccin, PrismVariant.Dark)

@Preview(name = "Console Light", group = "Prism/Ticket")
@Composable
internal fun TicketConsoleLightPreview() = TicketPreview(PrismThemeFamily.Console, PrismVariant.Light)

@Preview(name = "Console Dark", group = "Prism/Ticket")
@Composable
internal fun TicketConsoleDarkPreview() = TicketPreview(PrismThemeFamily.Console, PrismVariant.Dark)

@Composable
internal fun TicketPreview(family: PrismThemeFamily, variant: PrismVariant) {
  PrismTheme(family = family, variant = variant) {
    Column(
      Modifier.fillMaxSize().background(Prism.color.background).statusBarsPadding()
        .verticalScroll(rememberScrollState()).padding(Prism.dimens.spacingL),
      verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingL),
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS)) {
        Text("PRISM / COMPONENT STUDY", style = Prism.typography.overline, color = Prism.color.accent)
        Text("Ticket", style = Prism.typography.display, color = Prism.color.contentPrimary)
        Text(
          "${family.name} · ${variant.name}",
          style = Prism.typography.bodySmall,
          color = Prism.color.contentSecondary,
        )
      }
      TicketSample()
      Text(
        "A place for the occasion.\nA stub for what comes next.",
        style = Prism.typography.bodySmall,
        color = Prism.color.contentSecondary,
      )
    }
  }
}

@Composable
private fun TicketSample() {
  PrismTicket(
    modifier = Modifier.fillMaxWidth(),
    header = {
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("AMERICAS / 2026", style = Prism.typography.overline)
        Text("ONGOING", style = Prism.typography.overline, color = Prism.color.accent)
      }
    },
    stub = {
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        TicketDetail("EVENT DATES", "Aug 14 – Sep 12")
        TicketDetail("TEAMS", "12")
      }
      Spacer(Modifier.height(Prism.dimens.spacingM))
      PrismSurface(color = Prism.color.primaryAction, contentColor = Prism.color.onPrimaryAction) {
        Text(
          "YOUR NEXT CHAPTER",
          Modifier.fillMaxWidth().padding(Prism.dimens.spacingM),
          textAlign = TextAlign.Center,
          style = Prism.typography.button,
        )
      }
    },
  ) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
      PrismSurface(color = Prism.color.accentSubtle, contentColor = Prism.color.accent, shape = Prism.shapes.medium) {
        Column(
          Modifier.size(72.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
        ) {
          Text("VCT", style = Prism.typography.headline)
        }
      }
      Spacer(Modifier.height(Prism.dimens.spacingL))
      Text("VCT Americas\nStage 2", style = Prism.typography.headline, textAlign = TextAlign.Center)
      Spacer(Modifier.height(Prism.dimens.spacingS))
      Text("VALORANT Champions Tour", style = Prism.typography.label, color = Prism.color.accent)
      Spacer(Modifier.height(Prism.dimens.spacingL))
      Text(
        "The next great moment starts here.",
        style = Prism.typography.bodySmall,
        color = Prism.color.contentSecondary,
        textAlign = TextAlign.Center,
      )
    }
  }
}

@Composable
private fun TicketDetail(label: String, value: String) {
  Column(verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS)) {
    Text(label, style = Prism.typography.overline, color = Prism.color.contentSecondary)
    Text(value, style = Prism.typography.bodyLarge)
  }
}
