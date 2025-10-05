package dev.staticvar.vlr.remotesource.events

import dev.staticvar.vlr.remotesource.common.ApiPaths
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

internal class EventDataSourceImpl(private val client: HttpClient) : EventDataSource {
  override suspend fun list(): Result<List<EventListDto>> = runCatching {
    client.get(ApiPaths.EVENTS).body()
  }
  override suspend fun details(id: String): Result<EventDetailsDto> = runCatching {
    client.get(ApiPaths.event(id)).body()
  }
}
