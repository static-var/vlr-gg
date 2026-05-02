/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureevents.usecase

import dev.staticvar.vlr.domain.model.EventPreview
import dev.staticvar.vlr.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow

public class ObserveEventListUseCase(private val eventRepository: EventRepository) {
  public operator fun invoke(): Flow<List<EventPreview>> = eventRepository.getEvents()
}
