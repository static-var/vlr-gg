package dev.staticvar.vlr.featurerankings.usecase

import dev.staticvar.vlr.domain.model.RegionalRanking
import dev.staticvar.vlr.domain.repository.RankingsRepository
import kotlinx.coroutines.flow.Flow

public class ObserveRankingsUseCase(
  private val rankingsRepository: RankingsRepository,
) {
  public operator fun invoke(): Flow<List<RegionalRanking>> = rankingsRepository.getAllRankings()
}
