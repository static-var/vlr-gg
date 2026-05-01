package dev.staticvar.vlr.featurerankings.usecase

import dev.staticvar.vlr.domain.repository.RankingsRepository

public class RefreshRankingsUseCase(
  private val rankingsRepository: RankingsRepository,
) {
  public suspend operator fun invoke(): Result<Unit> = rankingsRepository.refreshRankings()
}
