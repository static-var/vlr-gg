package dev.staticvar.vlr.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.Ok
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.staticvar.vlr.data.VlrRepository
import dev.staticvar.vlr.utils.Waiting
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class VlrViewModel @Inject constructor(private val repository: VlrRepository) : ViewModel() {
  lateinit var action: Action

  private var _hideNavBar: MutableSharedFlow<Boolean> = MutableSharedFlow(0)
  val hideNavBar: SharedFlow<Boolean> = _hideNavBar

  private var _resetScroll: MutableSharedFlow<Boolean> = MutableSharedFlow(0)
  val resetScroll: SharedFlow<Boolean> = _resetScroll

  private var _selectedMatchTypePosition: MutableStateFlow<Int> = MutableStateFlow(0)
  val selectedMatchTypePosition: StateFlow<Int> = _selectedMatchTypePosition

  fun updateSelectedMatchTypePosition(position: Int) {
    viewModelScope.launch { _selectedMatchTypePosition.emit(position) }
  }

  private var _selectedEventTypePosition: MutableStateFlow<Int> = MutableStateFlow(0)
  val selectedEventTypePosition: StateFlow<Int> = _selectedEventTypePosition

  fun updateSelectedEventTypePosition(position: Int) {
    viewModelScope.launch { _selectedEventTypePosition.emit(position) }
  }

  fun hideNavbar(hide: Boolean) {
    viewModelScope.launch { _hideNavBar.emit(hide) }
  }

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

  fun parseNews(id: String) =
    repository.parseNews(id).stateIn(viewModelScope, SharingStarted.Lazily, null)

  fun trackMatch(id: String) = viewModelScope.launch { repository.addFavoriteMatch(id) }
  fun untrackMatch(id: String) = viewModelScope.launch { repository.removeFavoriteMatch(id) }

  fun trackEvent(id: String) =
    viewModelScope.launch { repository.addFavoriteEvent(id) }

  fun untrackEvent(id: String) =
    viewModelScope.launch { repository.removeFavoriteEvent(id) }

  fun trackTeam(id: String) = viewModelScope.launch { repository.addFavoriteTeam(id) }
  fun untrackTeam(id: String) = viewModelScope.launch { repository.removeFavoriteTeam(id) }

}
