package dev.staticvar.vlr.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.staticvar.vlr.data.NavState
import dev.staticvar.vlr.data.VlrRepository
import dev.staticvar.vlr.utils.Constants
import dev.staticvar.vlr.utils.TimeElapsed
import dev.staticvar.vlr.utils.Waiting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VlrViewModel @Inject constructor(private val repository: VlrRepository) : ViewModel() {

  private var _navState: MutableStateFlow<NavState> = MutableStateFlow(NavState.NEWS)
  val navState: StateFlow<NavState> = _navState

  fun setNavigation(state: NavState) {
    _navState.tryEmit(state)
  }

  lateinit var action: Action

  fun getMatchInfo(matchUrl: String) =
    repository.mergeMatchDetails(matchUrl).stateIn(viewModelScope, SharingStarted.Lazily, Waiting())

  fun getTournamentDetails(matchUrl: String) =
    repository.mergeEventDetails(matchUrl).stateIn(viewModelScope, SharingStarted.Lazily, Waiting())

  fun getTournaments() =
    repository.mergeEvents().stateIn(viewModelScope, SharingStarted.Lazily, Waiting())

  fun getAllMatches() =
    repository.mergeMatches().stateIn(viewModelScope, SharingStarted.Lazily, Waiting())

  fun getNews() = repository.mergeNews().stateIn(viewModelScope, SharingStarted.Lazily, Waiting())

  fun getTeamDetails(id: String) =
    repository.getTeamDetails(id).stateIn(viewModelScope, SharingStarted.Lazily, Waiting())

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

  fun getLatestAppVersion() = repository.getLatestAppVersion()
  fun getApkUrl() = repository.getApkUrl()
  fun downloadApkWithProgress(url: String) = repository.downloadApkWithProgress(url)
}
