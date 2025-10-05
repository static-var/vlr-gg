package dev.staticvar.vlr.remotesource.team

import dev.staticvar.vlr.remotesource.common.ApiPaths
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

interface TeamDataSource {
  suspend fun details(id: String): Result<TeamDetailsDto>
}
