package dev.staticvar.vlr.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.Ok
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.staticvar.vlr.data.VlrRepository
import dev.staticvar.vlr.utils.Waiting
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class VlrViewModel @Inject constructor(private val repository: VlrRepository) : ViewModel() {
  lateinit var action: Action

  private var _resetScroll: MutableSharedFlow<Boolean> = MutableSharedFlow(0)
  val resetScroll: SharedFlow<Boolean> = _resetScroll

  fun resetScroll() {
    viewModelScope.launch { _resetScroll.emit(true) }
  }

  fun postResetScroll() {
    viewModelScope.launch { _resetScroll.emit(false) }
  }

  fun refreshNews() =
    repository.updateLatestNews().stateIn(viewModelScope, SharingStarted.Lazily, Ok(false))

  fun getNews() =
    repository.getNewsFromDb().stateIn(viewModelScope, SharingStarted.Lazily, Waiting())

  fun refreshMatches() =
    repository.updateLatestMatches().stateIn(viewModelScope, SharingStarted.Lazily, Ok(false))

  fun getMatches() =
    repository.getMatchesFromDb().stateIn(viewModelScope, SharingStarted.Lazily, Waiting())

  fun refreshEvents() =
    repository.updateLatestEvents().stateIn(viewModelScope, SharingStarted.Lazily, Ok(false))

  fun getEvents() =
    repository.getEventsFromDb().stateIn(viewModelScope, SharingStarted.Lazily, Waiting())

  fun refreshRanks() =
    repository.updateLatestRanks().stateIn(viewModelScope, SharingStarted.Lazily, Ok(false))

  fun getRanks() =
    repository.getRanksFromDb().stateIn(viewModelScope, SharingStarted.Lazily, Waiting())

  fun refreshMatchInfo(id: String) =
    repository
      .updateLatestMatchDetails(id)
      .stateIn(viewModelScope, SharingStarted.Lazily, Ok(false))

  fun getMatchDetails(matchUrl: String) =
    repository
      .getMatchDetailsFromDb(matchUrl)
      .stateIn(viewModelScope, SharingStarted.Lazily, Waiting())

  fun refreshEventDetails(id: String) =
    repository
      .updateLatestEventDetails(id)
      .stateIn(viewModelScope, SharingStarted.Lazily, Ok(false))

  fun getEventDetails(matchUrl: String) =
    repository
      .getEventDetailsFromDb(matchUrl)
      .stateIn(viewModelScope, SharingStarted.Lazily, Waiting())

  fun refreshTeamDetails(id: String) =
    repository.getTeamDetails(id).stateIn(viewModelScope, SharingStarted.Lazily, Ok(false))

  fun getTeamDetails(id: String) =
    repository.getTeamDetailsFromDb(id).stateIn(viewModelScope, SharingStarted.Lazily, Waiting())

  fun refreshPlayerDetails(id: String) =
    repository.getPlayerDetails(id).stateIn(viewModelScope, SharingStarted.Lazily, Ok(false))

  fun getPlayerDetails(id: String) =
    repository.getPlayerDetailsFromDb(id).stateIn(viewModelScope, SharingStarted.Lazily, Waiting())

  fun trackTopic(topic: String) = viewModelScope.launch { repository.trackTopic(topic) }

  fun isTopicTracked(topic: String) = repository.isTopicTracked(topic)

  fun removeTopic(topic: String) = viewModelScope.launch { repository.removeTopic(topic) }

  fun parseNews(id: String) =
    repository.parseNews(id).stateIn(viewModelScope, SharingStarted.Lazily, null)
}
