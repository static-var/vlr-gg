package dev.staticvar.vlr.featurematches.usecase

import dev.staticvar.vlr.domain.repository.MatchRepository

public class RefreshMatchesUseCase(
  private val matchRepository: MatchRepository,
) {
  public suspend operator fun invoke(): Result<Unit> = matchRepository.refreshMatches()
}
