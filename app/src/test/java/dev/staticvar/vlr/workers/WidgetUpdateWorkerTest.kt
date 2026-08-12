package dev.staticvar.vlr.workers

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.google.common.truth.Truth.assertThat
import dev.staticvar.vlr.data.VlrRepository
import dev.staticvar.vlr.utils.WIDGET_WORK_KIND_KEY
import dev.staticvar.vlr.utils.areWidgetsEnabled
import dev.staticvar.vlr.utils.recordWidgetUpdateMillis
import dev.staticvar.vlr.utils.stopWorker
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(application = Application::class)
class WidgetUpdateWorkerTest {
  private lateinit var context: Context
  private lateinit var repository: VlrRepository

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    repository = mockk()
    every { repository.latestMatchesUpdatedAtMillis() } returns null
    mockkStatic("dev.staticvar.vlr.utils.WidgetHelperKt")
    coEvery { context.areWidgetsEnabled() } returns true
    coEvery { context.recordWidgetUpdateMillis(any()) } returns Unit
    every { context.stopWorker() } returns Unit
  }

  @After
  fun tearDown() {
    unmockkStatic("dev.staticvar.vlr.utils.WidgetHelperKt")
  }

  @Test
  fun `transient refresh failure retries without recording freshness`() = runTest {
    every { repository.updateLatestMatches() } returns
      flowOf<Result<Boolean, Throwable?>>(Err(IOException("offline")))

    val result = newWorker().doWork()

    assertThat(result).isEqualTo(ListenableWorker.Result.retry())
    coVerify(exactly = 0) { context.recordWidgetUpdateMillis(any()) }
  }

  @Test
  fun `one-time refresh stops retrying after three attempts`() = runTest {
    every { repository.updateLatestMatches() } returns
      flowOf<Result<Boolean, Throwable?>>(Err(IOException("offline")))

    val result = newWorker(runAttemptCount = 2).doWork()

    assertThat(result).isEqualTo(ListenableWorker.Result.failure())
  }

  @Test
  fun `periodic refresh defers transient failure after three attempts`() = runTest {
    every { repository.updateLatestMatches() } returns
      flowOf<Result<Boolean, Throwable?>>(Err(IOException("offline")))

    val result = newWorker(runAttemptCount = 2, periodic = true).doWork()

    assertThat(result).isEqualTo(ListenableWorker.Result.success())
  }

  @Test
  fun `legacy untyped refresh defers terminal failure to the next interval`() = runTest {
    every { repository.updateLatestMatches() } returns
      flowOf<Result<Boolean, Throwable?>>(Err(IllegalArgumentException("invalid response")))

    val result = newWorker(legacyUntyped = true).doWork()

    assertThat(result).isEqualTo(ListenableWorker.Result.success())
  }

  @Test
  fun `periodic refresh defers non-transient failure to the next interval`() = runTest {
    every { repository.updateLatestMatches() } returns
      flowOf<Result<Boolean, Throwable?>>(Err(IllegalArgumentException("invalid response")))

    val result = newWorker(periodic = true).doWork()

    assertThat(result).isEqualTo(ListenableWorker.Result.success())
  }

  @Test
  fun `cached refresh records the original freshness timestamp`() = runTest {
    val cachedRefreshMillis = 1_234L
    every { repository.updateLatestMatches() } returns flowOf()
    every { repository.latestMatchesUpdatedAtMillis() } returns cachedRefreshMillis

    val result = newWorker().doWork()

    assertThat(result).isEqualTo(ListenableWorker.Result.success())
    coVerify(exactly = 1) { context.recordWidgetUpdateMillis(cachedRefreshMillis) }
  }

  @Test
  fun `concurrent widget workers serialize repository refreshes`() = runTest {
    val activeRefreshes = AtomicInteger()
    val maximumActiveRefreshes = AtomicInteger()
    val firstRefreshStarted = CompletableDeferred<Unit>()
    val releaseFirstRefresh = CompletableDeferred<Unit>()
    every { repository.updateLatestMatches() } answers {
      flow {
        val active = activeRefreshes.incrementAndGet()
        maximumActiveRefreshes.updateAndGet { maximum -> maxOf(maximum, active) }
        if (!firstRefreshStarted.isCompleted) {
          firstRefreshStarted.complete(Unit)
          releaseFirstRefresh.await()
        }
        emit(Ok(false))
        activeRefreshes.decrementAndGet()
      }
    }

    val firstWorker = async { newWorker().doWork() }
    firstRefreshStarted.await()
    val secondWorker = async { newWorker().doWork() }
    yield()
    releaseFirstRefresh.complete(Unit)

    assertThat(firstWorker.await()).isEqualTo(ListenableWorker.Result.success())
    assertThat(secondWorker.await()).isEqualTo(ListenableWorker.Result.success())
    assertThat(maximumActiveRefreshes.get()).isEqualTo(1)
  }

  @Test
  fun `no widget cancels legacy work and succeeds`() = runTest {
    coEvery { context.areWidgetsEnabled() } returns false

    val result = newWorker().doWork()

    assertThat(result).isEqualTo(ListenableWorker.Result.success())
    verify(exactly = 1) { context.stopWorker() }
    verify(exactly = 0) { repository.updateLatestMatches() }
  }

  @Test
  fun `completed refresh records freshness and succeeds`() = runTest {
    val refreshMillis = 1_234L
    every { repository.updateLatestMatches() } returns flowOf(Ok(true), Ok(false))
    every { repository.latestMatchesUpdatedAtMillis() } returns refreshMillis

    val result = newWorker().doWork()

    assertThat(result).isEqualTo(ListenableWorker.Result.success())
    coVerify(exactly = 1) { context.recordWidgetUpdateMillis(refreshMillis) }
  }

  private fun newWorker(
    runAttemptCount: Int = 0,
    periodic: Boolean = false,
    legacyUntyped: Boolean = false,
  ): WidgetUpdateWorker {
    val workerParameters = mockk<WorkerParameters>(relaxed = true)
    every { workerParameters.runAttemptCount } returns runAttemptCount
    every { workerParameters.inputData } returns
      if (legacyUntyped) {
        workDataOf()
      } else {
        workDataOf(WIDGET_WORK_KIND_KEY to if (periodic) "PERIODIC" else "ONE_TIME")
      }
    return WidgetUpdateWorker(
      appContext = context,
      workerParams = workerParameters,
      vlrRepository = repository,
    )
  }
}
