/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureevents.usecase

import dev.staticvar.vlr.domain.model.EventDetails
import dev.staticvar.vlr.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow

public class ObserveEventDetailsUseCase(private val eventRepository: EventRepository) {
  public operator fun invoke(eventId: String): Flow<EventDetails?> = eventRepository.getEventDetails(eventId)
}
