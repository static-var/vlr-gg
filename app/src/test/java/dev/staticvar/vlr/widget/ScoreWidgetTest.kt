package dev.staticvar.vlr.widget

import androidx.glance.appwidget.SizeMode
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ScoreWidgetTest {
  @Test
  fun `widget uses exact size mode`() {
    assertThat(ScoreWidget().sizeMode).isEqualTo(SizeMode.Exact)
  }
}
