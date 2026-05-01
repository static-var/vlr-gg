package dev.staticvar.vlr.featurematches.usecase

import dev.staticvar.vlr.domain.model.MatchDetails
import dev.staticvar.vlr.domain.repository.MatchRepository
import kotlinx.coroutines.flow.Flow

public class ObserveMatchDetailsUseCase(
  private val matchRepository: MatchRepository,
) {
  public operator fun invoke(matchId: String): Flow<MatchDetails?> = matchRepository.getMatchDetails(matchId)
}
