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
import dev.staticvar.vlr.utils.i
import dev.staticvar.vlr.widget.ScoreWidget
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take

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
      vlrRepository.mergeMatches().onEach { i { "Updating widget" } }.take(1)
      ScoreWidget(vlrRepository).updateAll(appContext)
    }
    return Result.success()
  }
}
