# Freshness fixes and code

Updated September 7 after migrating all nine data ViewModels to AndroidX `ViewModel` and `viewModelScope`. All nine data screens observe the database and retain cached content during refresh. No migration files were added. News ordering, paused event status and map count fixes remain in place.

## 1. Refresh every data screen, including populated cached details

Each detail ViewModel belongs to one navigation entry and receives its ID in its constructor. There are no mutable current IDs, generation counters, `open…` methods or paired initialization effects. Koin supplies the ID once in [AppNavigationModule.kt](/Users/staticvar/Projects/vlr-gg/shared/src/commonMain/kotlin/dev/staticvar/vlr/shared/navigation/AppNavigationModule.kt:69):

```kotlin
val viewModel = koinViewModel<NewsArticleViewModel> { parametersOf(route.articleId) }
val uiState by viewModel.uiState.collectAsStateWithLifecycle()
RefreshWhenResumed(isOnline = viewModel.isOnline, onRefresh = viewModel::refresh)
```

The ViewModel combines DB content and refresh state. DB observation starts independently, so populated cached details are visible even offline. Example from [NewsArticleViewModel.kt](/Users/staticvar/Projects/vlr-gg/feature-news/src/commonMain/kotlin/dev/staticvar/vlr/featurenews/presentation/article/NewsArticleViewModel.kt:18):

```kotlin
public class NewsArticleViewModel(
  articleId: String,
  observeNewsArticleUseCase: ObserveNewsArticleUseCase,
  refreshNewsArticleUseCase: RefreshNewsArticleUseCase,
  networkMonitor: NetworkMonitor,
) : ViewModel() {
  private val refresher: RefreshController = RefreshController(viewModelScope, networkMonitor) {
    refreshNewsArticleUseCase(articleId)
  }

  public val uiState: StateFlow<NewsArticleUiState> =
    combine(observeNewsArticleUseCase(articleId), refresher.state) { article, refresh ->
      NewsArticleUiState(
        article = article,
        isLoading = false,
        isRefreshing = refresh.isRefreshing,
        errorMessage = refresh.errorMessage,
      )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, NewsArticleUiState())

  public fun refresh() {
    refresher.refresh()
  }
}
```

The same structure is used for news list, matches list/detail, events list/detail, rankings and team/player details. List filters and match preferences remain separate state inputs to `combine`.

## 2. Inline progress and refresh failures

[SharedRefreshStatus.kt](/Users/staticvar/Projects/vlr-gg/shared-ui/src/commonMain/kotlin/dev/staticvar/vlr/sharedui/component/common/SharedRefreshStatus.kt:25) animates between idle, progress and retry states with a fade and height transition using the theme's standard motion preset. Each screen groups its title and status together; the animated status includes its top spacing so the parent layout does not insert or remove an extra gap abruptly. It displays failures with Retry while cached content stays visible. Successful repository writes reach the UI through DB observation.

```kotlin
SharedRefreshStatus(
  isRefreshing = uiState.isRefreshing,
  errorMessage = uiState.errorMessage,
  onRefresh = onRefresh,
)
```

## 3. Online resume/reconnect, including reconnect during an active request

Lifecycle observation belongs to the navigation entry. The effect subscribes to connectivity while that entry is resumed. It only requests refresh; the ViewModel owns execution and error state. `refreshRequests`, `wasReady`, `pending`, and composable `isRefreshing` feedback are removed.

[RefreshWhenResumed.kt](/Users/staticvar/Projects/vlr-gg/shared/src/commonMain/kotlin/dev/staticvar/vlr/shared/navigation/RefreshWhenResumed.kt:19):

```kotlin
@Composable
internal fun RefreshWhenResumed(isOnline: StateFlow<Boolean>, onRefresh: () -> Unit) {
  val lifecycle = LocalLifecycleOwner.current.lifecycle
  val refresh by rememberUpdatedState(onRefresh)

  LaunchedEffect(lifecycle, isOnline) {
    lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
      isOnline.filter { it }.collect { refresh() }
    }
  }
}
```

The effect receives connectivity explicitly from the constructor-injected screen ViewModel. It performs no service lookup. Appearance also uses a constructor-injected `AppearanceViewModel`; the app root and Settings destination resolve their own instances at their respective boundaries, while sharing the repository's settings flow. UI composables receive state and callbacks.

`LocalLifecycleOwner` is the navigation entry's owner. `repeatOnLifecycle` cancels the connectivity subscription below RESUMED; `LaunchedEffect` cancels on disposal. No Activity is passed to a ViewModel or retained by the connectivity service.

Each ViewModel owns one [RefreshController.kt](/Users/staticvar/Projects/vlr-gg/core/src/commonMain/kotlin/dev/staticvar/vlr/core/refresh/RefreshController.kt:20):

```kotlin
class RefreshController(
  scope: CoroutineScope,
  networkMonitor: NetworkMonitor,
  action: suspend () -> Result<Unit>,
) {
  private val requests = Channel<Unit>(Channel.CONFLATED)
  private val mutableState = MutableStateFlow(RefreshState())
  val state: StateFlow<RefreshState> = mutableState.asStateFlow()

  init {
    scope.launch {
      for (request in requests) {
        networkMonitor.isOnline.first { it }
        mutableState.value = RefreshState(isRefreshing = true)
        try {
          action().getOrThrow()
          currentCoroutineContext().ensureActive()
        } catch (cancellation: CancellationException) {
          throw cancellation
        } catch (error: Exception) {
          currentCoroutineContext().ensureActive()
          mutableState.update { it.copy(errorMessage = error.message ?: "Could not refresh data") }
        } finally {
          mutableState.update { it.copy(isRefreshing = false) }
        }
      }
    }.invokeOnCompletion { requests.close() }
  }

  fun refresh() {
    requests.trySend(Unit)
  }
}
```

A conflated channel holds at most one queued request. Requests arriving during active work become a follow-up. The worker waits for connectivity before starting work. Cancellation is propagated; ordinary failures stop progress and remain retryable. Finishing a request does not itself trigger another request.

Covering/backgrounding an entry stops new lifecycle-triggered requests while its existing work may finish. Popping the entry clears its AndroidX `ViewModelStore`, which cancels `viewModelScope` and its observation and refresh work. The former manual Koin navigation scopes and ViewModel `clear()` methods are removed. Tests register ViewModels in a `ViewModelStore` and clear the store to verify cancellation.

[AppNavHost.kt](/Users/staticvar/Projects/vlr-gg/shared/src/commonMain/kotlin/dev/staticvar/vlr/shared/navigation/AppNavHost.kt:45) shares the standard decorators across both responsive navigation layouts:

```kotlin
val entryDecorators = listOf(
  rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
  rememberViewModelStoreNavEntryDecorator<NavKey>(),
)
```

Koin `viewModel` definitions construct the instances; `koinViewModel` resolves them from the current entry's store. Feature modules use Google's multiplatform AndroidX ViewModel core. The Compose and Navigation 3 adapters use JetBrains' multiplatform artifacts, including their iOS variants.

Connectivity is an injected `StateFlow<Boolean>`. [AndroidNetworkMonitor.kt](/Users/staticvar/Projects/vlr-gg/shared/src/androidMain/kotlin/dev/staticvar/vlr/shared/network/AndroidNetworkMonitor.kt:37) uses a default-network callback and the application context. [IosNetworkMonitor.kt](/Users/staticvar/Projects/vlr-gg/shared/src/iosMain/kotlin/dev/staticvar/vlr/shared/network/IosNetworkMonitor.kt:39) uses `NWPathMonitor`. `callbackFlow` unregisters/cancels platform listeners in `awaitClose`; `WhileSubscribed` shares one listener and releases it when unused. Koin closure cancels the monitor's scope. Android uses callback-supplied capabilities, following the [Android callback contract](https://developer.android.com/reference/android/net/ConnectivityManager.NetworkCallback).

The desktop monitor/polling implementation was removed. No desktop fallback or compatibility implementation was added.

## 4. News order follows the API

[News.sq](/Users/staticvar/Projects/vlr-gg/local-source/src/commonMain/sqldelight/dev/staticvar/vlr/localsource/database/News.sq:12) stores the response position. [NewsRepositoryImpl.kt](/Users/staticvar/Projects/vlr-gg/data/src/commonMain/kotlin/dev/staticvar/vlr/data/repository/NewsRepositoryImpl.kt:68) persists each index for inserts and updates; article refresh preserves it.

```kotlin
dtos.forEachIndexed { index, dto ->
  val entity = dto.toEntity(listPosition = index.toLong())
  // The insert/update persists entity.list_position.
}
```

```sql
SELECT * FROM news
ORDER BY list_position IS NULL, list_position, date DESC;
```

The regression covers same-day nonnumeric IDs, changed response order and article refresh retaining order. Existing article data is reread within the write transaction to retain a concurrent feed update's position.

## 5. Paused and unknown events retain their status

[ApiEnums.kt](/Users/staticvar/Projects/vlr-gg/remote-source/src/commonMain/kotlin/dev/staticvar/vlr/remotesource/common/ApiEnums.kt:45) recognizes `PAUSED("paused")`. Unsupported wire values store `"UNKNOWN"` instead of `"UPCOMING"`:

```kotlin
status = status?.name ?: "UNKNOWN",
```

Domain values, tags and filters support Paused/Unknown. A selected Paused/Unknown filter remains visible when its results become empty.

## 6. Known map count displays before statistics exist

[MatchDetailFormatting.kt](/Users/staticvar/Projects/vlr-gg/shared-ui/src/commonMain/kotlin/dev/staticvar/vlr/sharedui/component/match/detail/MatchDetailFormatting.kt:75):

```kotlin
internal fun MatchDetails.matchDetailMapCountStat(): String = when {
  mapCount == 1 -> "1 map"
  mapCount > 1 -> "$mapCount maps"
  else -> "-"
}
```

## Verification

- After removing composable service lookups, Android/shared iOS compilation and native iOS build/launch passed. Settings switched to Catppuccin with the updated palette visible across the screen, then switched to Console and restored the original Brutalist selection. No `koinInject` calls remain in shared composables.

- After the AndroidX migration, all 60 tests across the six feature modules passed on iOS Simulator; shared iOS/Android and Android app compilation also passed. The lifecycle test verifies that clearing the store cancels active refresh, skips queued refresh, and stops DB observation.
- The preceding refresh refactor passed 74 tests across core and the six feature modules. These cover cached/empty content, failed refresh/retry, queue coalescing, offline waiting, scope cancellation, filters and preferences.
- After the AndroidX migration, the iOS app built, installed and launched on the existing fresh QA simulator. Native navigation opened the correct article from News and returned to the populated list.
- Native smoke check opened the article through its constructor-supplied ID. Background/foreground kept PID 56479 and advanced its saved news timestamp from 1788721380192 to 1788721398854 while retaining all 30 rows. See [refactor resume evidence](refactor-resume-verification.json).
- No desktop targets were built for this refactor. No migrations or diagnostic instrumentation were added.
- Earlier native checks verified API news order, five head-to-head encounters for match 734308, and Paused labeling for event 2634. Screenshots: [news order](news-order.jpg), [match details](match-details.jpg), [head-to-head](match-history.jpg), [paused event](paused-event.jpg).

The previously reported intermittent Compose iOS accessibility crash during rapid automated Rankings → Global Esports → Back navigation remains unresolved. This refactor does not claim to fix it. Device connectivity toggling and VoiceOver behavior have not been verified.
