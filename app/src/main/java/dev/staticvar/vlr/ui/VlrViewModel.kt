package dev.staticvar.vlr.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.Ok
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

  private var _navState: MutableStateFlow<NavState> = MutableStateFlow(NavState.NEWS_OVERVIEW)
  val navState: StateFlow<NavState> = _navState

  fun setNavigation(state: NavState) {
    _navState.tryEmit(state)
  }

  lateinit var action: Action

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

  fun getTeamDetails(id: String) =
    repository.getTeamDetails(id).stateIn(viewModelScope, SharingStarted.Lazily, Waiting())

  fun trackTopic(topic: String) =
    viewModelScope.launch(Dispatchers.IO) { repository.trackTopic(topic) }

  fun isTopicTracked(topic: String) = repository.isTopicTracked(topic)

  fun removeTopic(topic: String) =
    viewModelScope.launch(Dispatchers.IO) { repository.removeTopic(topic) }

  fun parseNews(id: String) =
    repository.parseNews(id).stateIn(viewModelScope, SharingStarted.Lazily, Waiting())

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
