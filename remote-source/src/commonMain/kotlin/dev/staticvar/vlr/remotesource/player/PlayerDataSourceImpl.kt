package dev.staticvar.vlr.remotesource.player

import dev.staticvar.vlr.remotesource.common.ApiPaths
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

internal class PlayerDataSourceImpl(private val client: HttpClient) : PlayerDataSource {
  override suspend fun details(id: String): Result<PlayerDetailsDto> = runCatching {
    client.get(ApiPaths.player(id)).body()
  }
}
