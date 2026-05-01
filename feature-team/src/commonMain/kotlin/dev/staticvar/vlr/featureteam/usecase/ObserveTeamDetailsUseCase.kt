package dev.staticvar.vlr.featureteam.usecase

import dev.staticvar.vlr.domain.model.TeamInfo
import dev.staticvar.vlr.domain.repository.TeamRepository
import kotlinx.coroutines.flow.Flow

public class ObserveTeamDetailsUseCase(
  private val teamRepository: TeamRepository,
) {
  public operator fun invoke(teamId: String): Flow<TeamInfo?> = teamRepository.getTeamDetails(teamId)
}
