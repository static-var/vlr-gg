/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureevents.presentation.mascot

import dev.staticvar.vlr.domain.model.EventDetails
import dev.staticvar.vlr.domain.model.EventStatus
import dev.staticvar.vlr.sharedui.mascot.MascotCue

/** Celebrates a favorite champion only after the event has a confirmed, unique first place. */
public fun eventMascotCues(event: EventDetails, favoriteTeamIds: Set<String>): List<MascotCue> {
  if (event.id.isBlank() || event.status != EventStatus.COMPLETED) return emptyList()
  val champion = event.prizes.singleOrNull {
    it.position.trim().lowercase() in setOf("1", "1st", "1st place")
  }?.team ?: return emptyList()
  if (champion.id.isNullOrBlank() || champion.id !in favoriteTeamIds || champion.name.isBlank()) {
    return emptyList()
  }
  return listOf(
    MascotCue(
      id = "event:${event.id}:winner:${champion.id}",
      message = "${champion.name} won the event!",
      priority = 50,
    ),
  )
}
