package dev.staticvar.vlr.widget

import android.app.Application
import android.content.Context
import android.os.Looper
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.AppWidgetId
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(application = Application::class)
class WidgetWorkSchedulingTest {
  private lateinit var context: Context
  private lateinit var workManager: WorkManager

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()

    val synchronousExecutor = SynchronousExecutor()
    val configuration =
      Configuration.Builder()
        .setExecutor(synchronousExecutor)
        .setTaskExecutor(synchronousExecutor)
        .build()
    WorkManagerTestInitHelper.initializeTestWorkManager(context, configuration)
    workManager = WorkManager.getInstance(context)
  }

  @Test
  fun `refresh action replaces an existing immediate refresh`() = runTest {
    val action = RefreshWidgetAction()

    action.onAction(context, AppWidgetId(1), actionParametersOf())
    val firstRefresh = workManager.getWorkInfosForUniqueWork(WIDGET_REFRESH_WORK).get().single()

    action.onAction(context, AppWidgetId(1), actionParametersOf())

    val refreshWork = workManager.getWorkInfosForUniqueWork(WIDGET_REFRESH_WORK).get().single()
    assertThat(refreshWork.id).isNotEqualTo(firstRefresh.id)
    assertThat(refreshWork.state).isEqualTo(WorkInfo.State.ENQUEUED)
  }

  @Test
  fun `receiver restores widget work`() = runTest {
    val receiver = ScoreWidgetReceiver()

    receiver.onRestored(context, intArrayOf(1), intArrayOf(2))
    shadowOf(Looper.getMainLooper()).idle()

    assertThat(workManager.getWorkInfosForUniqueWork(WIDGET_PERIODIC_WORK).get().single().state)
      .isEqualTo(WorkInfo.State.ENQUEUED)
    assertThat(workManager.getWorkInfosForUniqueWork(WIDGET_REFRESH_WORK).get().single().state)
      .isEqualTo(WorkInfo.State.ENQUEUED)
  }

  @Test
  fun `receiver schedules and cancels widget work with widget lifecycle`() = runTest {
    val receiver = ScoreWidgetReceiver()

    receiver.onEnabled(context)
    shadowOf(Looper.getMainLooper()).idle()

    val periodicWork = workManager.getWorkInfosForUniqueWork(WIDGET_PERIODIC_WORK).get()
    assertThat(periodicWork).hasSize(1)
    assertThat(periodicWork.single().state).isEqualTo(WorkInfo.State.ENQUEUED)

    val refreshWork = workManager.getWorkInfosForUniqueWork(WIDGET_REFRESH_WORK).get()
    assertThat(refreshWork).hasSize(1)
    assertThat(refreshWork.single().state).isEqualTo(WorkInfo.State.ENQUEUED)

    receiver.onDisabled(context)
    shadowOf(Looper.getMainLooper()).idle()

    assertThat(workManager.getWorkInfosForUniqueWork(WIDGET_PERIODIC_WORK).get().single().state)
      .isEqualTo(WorkInfo.State.CANCELLED)
    assertThat(workManager.getWorkInfosForUniqueWork(WIDGET_REFRESH_WORK).get().single().state)
      .isEqualTo(WorkInfo.State.CANCELLED)
  }

  private companion object {
    const val WIDGET_PERIODIC_WORK = "widget_update"
    const val WIDGET_REFRESH_WORK = "widget_refresh"
  }
}
