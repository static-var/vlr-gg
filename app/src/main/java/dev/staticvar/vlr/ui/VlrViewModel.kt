package dev.staticvar.vlr.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.staticvar.vlr.data.NavState
import dev.staticvar.vlr.data.VlrRepository
import dev.staticvar.vlr.utils.Constants
import dev.staticvar.vlr.utils.TimeElapsed
import dev.staticvar.vlr.utils.Waiting
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class VlrViewModel @Inject constructor(private val repository: VlrRepository) : ViewModel() {

  private var _navState: MutableStateFlow<NavState> = MutableStateFlow(NavState.NEWS)
  val navState: StateFlow<NavState> = _navState

  fun setNavigation(state: NavState) {
    _navState.tryEmit(state)
  }

  lateinit var action: Action

  fun getMatchInfo(matchUrl: String) =
      repository
          .getMatchInfo(matchUrl)
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), Waiting())

  fun getTournamentDetails(matchUrl: String) =
      repository
          .getTournamentInfo(matchUrl)
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), Waiting())

  fun getTournaments() =
      repository
          .getTournaments()
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), Waiting())

  fun getAllMatches() =
      repository
          .getAllMatchesPreview()
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), Waiting())

  fun getNews() =
      repository.getAllNews().stateIn(viewModelScope, SharingStarted.WhileSubscribed(), Waiting())

  fun trackTopic(topic: String) =
      viewModelScope.launch(Dispatchers.IO) { repository.trackTopic(topic) }

  fun isTopicTracked(topic: String) = repository.isTopicTracked(topic)

  fun removeTopic(topic: String) =
      viewModelScope.launch(Dispatchers.IO) { repository.removeTopic(topic) }

  fun clearCache() {
    TimeElapsed.reset(Constants.KEY_UPCOMING)
    TimeElapsed.reset(Constants.KEY_COMPLETED)
    TimeElapsed.reset(Constants.KEY_TOURNAMENT_ALL)
    TimeElapsed.reset(Constants.KEY_TOURNAMENT_ALL)
    TimeElapsed.reset(Constants.KEY_NEWS)
  }

  private val _forceRefreshCounter = MutableStateFlow(0)
  val forceRefreshCounter: StateFlow<Int> = _forceRefreshCounter

  fun updateCounter() {
    _forceRefreshCounter.value++
  }
}
