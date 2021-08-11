package dev.unusedvariable.vlr.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.unusedvariable.vlr.data.NavState
import dev.unusedvariable.vlr.data.VlrRepository
import dev.unusedvariable.vlr.data.dao.MatchDetailsDao
import dev.unusedvariable.vlr.utils.Constants
import dev.unusedvariable.vlr.utils.TimeElapsed
import dev.unusedvariable.vlr.utils.Waiting
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class VlrViewModel @Inject constructor(
    private val repository: VlrRepository
) : ViewModel() {

    private var _navState: MutableStateFlow<NavState> = MutableStateFlow(NavState.UPCOMING)
    val navState: StateFlow<NavState> = _navState

    fun setNavigation(state: NavState) {
        _navState.value = state
    }

    lateinit var action: Action

    fun getUpcomingMatches() = repository.getUpcomingMatches()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), Waiting())

    fun getPreviousMatches() = repository.getCompletedMatches()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), Waiting())

    fun getMatchDetails(matchUrl: String) = repository.getMatchDetail(matchUrl)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), Waiting())


//    fun matchDetails(url: String) = flow<Operation<MatchDetails>> {
//        emit(Waiting())
//        repository.matchDetails(url).stream(StoreRequest.cached("", true)).collect { response ->
//            when (response) {
//                is StoreResponse.Loading -> emit(Waiting())
//                is StoreResponse.Data -> emit(Pass(response.value))
//                is StoreResponse.Error.Exception,
//                is StoreResponse.Error.Message -> emit(
//                    Fail(
//                        response.errorMessageOrNull() ?: "Unable to fetch data"
//                    )
//                )
//            }
//        }
//    }

    fun clearCache() {
        TimeElapsed.reset(Constants.KEY_UPCOMING)
        TimeElapsed.reset(Constants.KEY_COMPLETED)
    }

    private val _forceRefreshCounter = MutableStateFlow(0)
    val forceRefreshCounter: StateFlow<Int> = _forceRefreshCounter

    fun updateCounter() {
        _forceRefreshCounter.value++
    }

}