package dev.staticvar.vlr.featurematches.usecase

import dev.staticvar.vlr.domain.repository.MatchRepository

public class RefreshMatchDetailsUseCase(
  private val matchRepository: MatchRepository,
) {
  public suspend operator fun invoke(matchId: String): Result<Unit> = matchRepository.refreshMatchDetails(matchId)
}
