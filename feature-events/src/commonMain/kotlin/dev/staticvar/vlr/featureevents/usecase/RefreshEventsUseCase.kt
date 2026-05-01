package dev.staticvar.vlr.featureevents.usecase

import dev.staticvar.vlr.domain.repository.EventRepository

public class RefreshEventsUseCase(
  private val eventRepository: EventRepository,
) {
  public suspend operator fun invoke(): Result<Unit> = eventRepository.refreshEvents()
}
