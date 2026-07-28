package dev.staticvar.vlr.workers

import android.content.Context
import androidx.core.content.edit
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.firebase.messaging.FirebaseMessaging
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.staticvar.vlr.data.dao.EventFavDao
import dev.staticvar.vlr.data.dao.MatchFavDao
import dev.staticvar.vlr.data.dao.TeamFavDao
import dev.staticvar.vlr.utils.e
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

@HiltWorker
class FirebaseTopicSyncWorker
@AssistedInject
constructor(
  @Assisted appContext: Context,
  @Assisted workerParams: WorkerParameters,
  private val matchFavDao: MatchFavDao,
  private val eventFavDao: EventFavDao,
  private val teamFavDao: TeamFavDao,
) : CoroutineWorker(appContext, workerParams) {

  override suspend fun doWork(): Result {
    val installationId = inputData.getString(INPUT_INSTALLATION_ID) ?: return Result.failure()
    val preferences =
      applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    if (preferences.getString(LAST_SYNCED_INSTALLATION_ID, null) == installationId) {
      return Result.success()
    }

    return try {
      trackedTopics().forEach { topic ->
        FirebaseMessaging.getInstance().subscribeToTopic(topic).await()
      }

      preferences.edit(commit = true) {
        putString(LAST_SYNCED_INSTALLATION_ID, installationId)
      }
      Result.success()
    } catch (cancellation: CancellationException) {
      throw cancellation
    } catch (throwable: Exception) {
      e(message = { "Firebase topic synchronization failed" }, throwable = throwable)
      Result.retry()
    }
  }

  private suspend fun trackedTopics(): Set<String> =
    buildSet {
      matchFavDao.getFavoriteMatches().first().mapTo(this) { favorite -> "match-${favorite.id}" }
      eventFavDao.getFavoriteEvents().first().mapTo(this) { favorite -> "event-${favorite.id}" }
      teamFavDao.getFavoriteTeams().first().mapTo(this) { favorite -> "team-${favorite.id}" }
    }.filterTo(mutableSetOf()) { topic -> topic.substringAfter('-').isNotBlank() }
}

fun Context.enqueueFirebaseTopicSync(installationId: String) {
  val workRequest =
    OneTimeWorkRequestBuilder<FirebaseTopicSyncWorker>()
      .setConstraints(
        Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
      )
      .setInputData(workDataOf(INPUT_INSTALLATION_ID to installationId))
      .build()

  WorkManager.getInstance(this)
    .enqueueUniqueWork(FIREBASE_TOPIC_SYNC_WORK, ExistingWorkPolicy.REPLACE, workRequest)
}

private const val FIREBASE_TOPIC_SYNC_WORK = "firebase_topic_sync"
private const val INPUT_INSTALLATION_ID = "firebase_installation_id"
private const val PREFERENCES_NAME = "firebase_topic_sync"
private const val LAST_SYNCED_INSTALLATION_ID = "last_synced_installation_id"
