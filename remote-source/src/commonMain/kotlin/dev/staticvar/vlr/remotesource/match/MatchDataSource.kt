package dev.staticvar.vlr.remotesource.match

interface MatchDataSource {
  suspend fun list(): Result<List<MatchPreviewDto>>
  suspend fun details(id: String): Result<MatchDetailsDto>
}
