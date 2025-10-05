package dev.staticvar.vlr.remotesource.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

/**
 * API service for fetching Match data from backend.
 * Backend scrapes VLR.gg and returns JSON.
 */
interface MatchApiService {
  /**
   * Fetch all match previews (overview list).
   */
  suspend fun getMatches(): Result<List<MatchPreviewDto>>
  
  /**
   * Fetch detailed match information by ID.
   */
  suspend fun getMatchDetails(matchId: String): Result<MatchDetailsDto>
}

internal class MatchApiServiceImpl(
  private val httpClient: HttpClient,
) : MatchApiService {
  
  override suspend fun getMatches(): Result<List<MatchPreviewDto>> = runCatching {
    httpClient.get("/api/v1/matches/").body<List<MatchPreviewDto>>()
  }
  
  override suspend fun getMatchDetails(matchId: String): Result<MatchDetailsDto> = runCatching {
    httpClient.get("/api/v1/matches/$matchId").body<MatchDetailsDto>()
  }
}
