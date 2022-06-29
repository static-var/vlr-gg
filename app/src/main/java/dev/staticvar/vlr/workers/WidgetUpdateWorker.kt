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
import dev.staticvar.vlr.widget.ScoreWidget
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach

@HiltWorker
class WidgetUpdateWorker
@AssistedInject
constructor(
  @Assisted private val appContext: Context,
  @Assisted workerParams: WorkerParameters,
  private val vlrRepository: VlrRepository
) : CoroutineWorker(appContext, workerParams) {
  override suspend fun doWork(): Result {
    val widgetsEnabled = appContext.areWidgetsEnabled()
    if (widgetsEnabled) {
      vlrRepository.updateLatestMatches().onEach { i { "Updating widget $it" } }.collect()
      ScoreWidget(vlrRepository).updateAll(appContext)
    } else e { "No Widget to update" }
    return Result.success()
  }
}
