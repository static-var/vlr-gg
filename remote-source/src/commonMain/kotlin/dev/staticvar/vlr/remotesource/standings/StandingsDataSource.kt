package dev.staticvar.vlr.remotesource.standings

import dev.staticvar.vlr.remotesource.common.ApiPaths
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

interface StandingsDataSource {
  suspend fun byYear(year: Int): Result<StandingsDto>
}
