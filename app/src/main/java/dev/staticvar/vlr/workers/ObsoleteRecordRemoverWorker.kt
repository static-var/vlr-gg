package dev.staticvar.vlr.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.staticvar.vlr.data.VlrRepository
import java.util.concurrent.TimeUnit

@HiltWorker
class ObsoleteRecordRemoverWorker
@AssistedInject
constructor(
  @Assisted private val appContext: Context,
  @Assisted workerParams: WorkerParameters,
  private val vlrRepository: VlrRepository
) : CoroutineWorker(appContext, workerParams) {
  override suspend fun doWork(): Result {
    println("Deleting obsolete records")
    vlrRepository.deleteObsoleteRecords()
    return Result.success()
  }
}

fun Context.queueObsoleteRecord() {
  val constraints = Constraints.Builder().setRequiresCharging(true).build()
  val work =
    PeriodicWorkRequestBuilder<ObsoleteRecordRemoverWorker>(12, TimeUnit.HOURS)
      .setConstraints(constraints)
      .build()
  WorkManager.getInstance(this)
    .enqueueUniquePeriodicWork("obsolete_records_deleter", ExistingPeriodicWorkPolicy.REPLACE, work)
}
