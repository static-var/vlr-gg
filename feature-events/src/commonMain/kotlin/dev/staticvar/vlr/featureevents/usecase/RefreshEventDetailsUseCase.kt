package dev.staticvar.vlr.featureevents.usecase

import dev.staticvar.vlr.domain.repository.EventRepository

public class RefreshEventDetailsUseCase(
  private val eventRepository: EventRepository,
) {
  public suspend operator fun invoke(eventId: String): Result<Unit> = eventRepository.refreshEventDetails(eventId)
}
