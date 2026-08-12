package dev.staticvar.vlr.workers

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.staticvar.vlr.data.VlrRepository
import dev.staticvar.vlr.utils.areWidgetsEnabled
import dev.staticvar.vlr.utils.e
import dev.staticvar.vlr.utils.i
import dev.staticvar.vlr.utils.WidgetWorkKind
import dev.staticvar.vlr.utils.WIDGET_WORK_KIND_KEY
import dev.staticvar.vlr.utils.recordWidgetUpdateMillis
import dev.staticvar.vlr.utils.stopWorker
import dev.staticvar.vlr.widget.ScoreWidget
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.http.HttpStatusCode
import java.io.IOException
import java.util.concurrent.CancellationException
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@HiltWorker
class WidgetUpdateWorker
@AssistedInject
constructor(
  @Assisted private val appContext: Context,
  @Assisted workerParams: WorkerParameters,
  private val vlrRepository: VlrRepository,
) : CoroutineWorker(appContext, workerParams) {
  override suspend fun doWork(): Result {
    if (!appContext.areWidgetsEnabled()) {
      e { "No widget to update" }
      appContext.stopWorker()
      return Result.success()
    }

    return widgetRefreshMutex.withLock { refreshWidget() }
  }

  private suspend fun refreshWidget(): Result {
    return try {
      var attemptedNetworkRefresh = false
      var completedNetworkRefresh = false
      var refreshError: Throwable? = null

      vlrRepository.updateLatestMatches().collect { update ->
        i { "Updating widget $update" }
        if (update.isErr) {
          refreshError = update.error
        } else {
          attemptedNetworkRefresh = attemptedNetworkRefresh || update.value
          completedNetworkRefresh = completedNetworkRefresh || !update.value
        }
      }

      refreshError?.let(::workResultFor)
        ?: if (attemptedNetworkRefresh && !completedNetworkRefresh) {
          e { "Widget refresh ended before the network update completed" }
          terminalFailureResult()
        } else {
          val updateMillis =
            vlrRepository.latestMatchesUpdatedAtMillis()
              ?: System.currentTimeMillis().takeIf { completedNetworkRefresh }
          updateMillis?.let { appContext.recordWidgetUpdateMillis(it) }
          ScoreWidget().updateAll(appContext)
          Result.success()
        }
    } catch (exception: CancellationException) {
      throw exception
    } catch (exception: Throwable) {
      workResultFor(exception)
    }
  }

  private fun workResultFor(error: Throwable?): Result {
    if (error == null) {
      e { "Widget refresh failed without an error" }
      return terminalFailureResult()
    }

    e(message = { "Widget refresh failed" }, throwable = error)
    return if (error.isTransientRefreshFailure() && runAttemptCount < MAX_REFRESH_ATTEMPTS - 1) {
      Result.retry()
    } else {
      terminalFailureResult()
    }
  }

  private fun terminalFailureResult(): Result =
    if (inputData.getString(WIDGET_WORK_KIND_KEY) == WidgetWorkKind.ONE_TIME.name) {
      Result.failure()
    } else {
      Result.success()
    }

  private fun Throwable.isTransientRefreshFailure(): Boolean {
    return when (this) {
      is ServerResponseException -> true
      is ClientRequestException ->
        response.status == HttpStatusCode.RequestTimeout ||
          response.status == HttpStatusCode.TooManyRequests
      is IOException -> true
      else -> false
    }
  }

  private companion object {
    const val MAX_REFRESH_ATTEMPTS = 3
    val widgetRefreshMutex = Mutex()
  }
}
