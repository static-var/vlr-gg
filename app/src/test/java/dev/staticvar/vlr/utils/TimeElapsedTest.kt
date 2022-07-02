package dev.staticvar.vlr.utils

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.*
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
internal class TimeElapsedTest {

  private val key1 = "random_key1"
  private val key2 = "random_key2"
  private val duration1 = 5.seconds
  private val duration2 = 8.seconds

  @Before
  fun setup() {
    mockkStatic(Calendar::class)
    every { Calendar.getInstance().timeInMillis } returns 0
  }

  @After
  fun cleanup() {
    unmockkObject(Calendar::class)
  }

  @Test
  fun `test if start will add entry in the map`() {
    TimeElapsed.start(key1, duration1)
    assertThat(TimeElapsed.hasElapsed(key1)).isFalse()
  }

  @Test
  fun `test if start will override entry in the map`() {
    TimeElapsed.start(key1, duration1)
    assertThat(TimeElapsed.hasElapsed(key1)).isFalse()
    assertThat(TimeElapsed.timeForKey(key1)).isEqualTo(duration1.inWholeMilliseconds)
    TimeElapsed.start(key1, duration2)
    assertThat(TimeElapsed.timeForKey(key1)).isEqualTo(duration2.inWholeMilliseconds)
  }

  @Test
  fun `test if inserting second key changes duration for the first key`() {
    TimeElapsed.start(key1, duration1)
    TimeElapsed.start(key2, duration2)
    assertThat(TimeElapsed.timeForKey(key1)).isEqualTo(duration1.inWholeMilliseconds)
    assertThat(TimeElapsed.timeForKey(key2)).isEqualTo(duration2.inWholeMilliseconds)
  }

  @Test
  fun `test if resets the timing for a key`() {
    TimeElapsed.start(key1, duration1)
    assertThat(TimeElapsed.timeForKey(key1)).isEqualTo(duration1.inWholeMilliseconds)
    TimeElapsed.reset(key1)
    assertThat(TimeElapsed.timeForKey(key1)).isLessThan(0)
    assertThat(TimeElapsed.hasElapsed(key1)).isTrue()
  }

  @Test
  fun `test if key exists in hasElapsed after start is called`() {
    TimeElapsed.resetCache()
    assertThat(TimeElapsed.timeForKey(key1)).isNull()
    TimeElapsed.start(key1, duration1)
    assertThat(TimeElapsed.timeForKey(key1)).isNotNull()
  }
}
