package dev.staticvar.vlr.utils

import android.content.Context
import androidx.collection.lruCache
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.size.Size
import coil.size.SizeResolver
import com.materialkolor.DynamicMaterialTheme
import com.materialkolor.PaletteStyle
import com.materialkolor.ktx.harmonize
import com.materialkolor.ktx.themeColor
import dev.staticvar.vlr.ui.LocalColorExtractor
import dev.staticvar.vlr.ui.theme.VLRTheme
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class ColorExtractor(private val context: Context) {
  private val cache = lruCache<String, Color>(150)

  suspend fun calculatePrimaryColor(
    model: String,
    fallback: Color,
    sizeResolver: SizeResolver = DEFAULT_REQUEST_SIZE,
  ): Color {
    val cached = cache[model]
    if (cached != null) {
      return cached
    }

    val bitmap =
      suspendCancellableCoroutine<ImageBitmap> { cont ->
        val request =
          ImageRequest.Builder(context)
            .data(model)
            .size(sizeResolver)
            .allowHardware(enable = false)
            .allowRgb565(enable = true)
            .target(
              onSuccess = { result -> cont.resume(result.toBitmap().asImageBitmap()) },
              onError = { cont.resumeWithException(IllegalArgumentException()) },
            )
            .build()

        context.imageLoader.enqueue(request)
      }

    val suitableColors = bitmap.themeColor(fallback)
    return suitableColors.harmonize(fallback).also { cache.put(model, it) }
  }

  private companion object {
    val DEFAULT_REQUEST_SIZE = SizeResolver(Size(96, 96))
  }
}

@Composable
fun DynamicTheme(
  model: String,
  fallback: Color = VLRTheme.colorScheme.primary,
  useDarkTheme: Boolean = isSystemInDarkTheme(),
  style: PaletteStyle = PaletteStyle.Vibrant,
  content: @Composable () -> Unit,
) {
  val colorExtractor = LocalColorExtractor.current

  val color by
    produceState<Color?>(initialValue = fallback, model, colorExtractor) {
      val result = cancellableRunCatching { colorExtractor.calculatePrimaryColor(model, fallback) }
      value = result.getOrNull()
    }
  DynamicMaterialTheme(
    seedColor = color ?: fallback,
    useDarkTheme = useDarkTheme,
    animate = true,
    style = style,
    content = content,
  )
}

@Composable
fun rememberDynamicColor(
  model: String,
  fallback: Color = VLRTheme.colorScheme.primary,
): Color {
  val colorExtractor = LocalColorExtractor.current
  val color by
    produceState<Color?>(initialValue = fallback, model, colorExtractor) {
      val result = cancellableRunCatching { colorExtractor.calculatePrimaryColor(model, fallback) }
      value = result.getOrNull()
    }

  return color ?: fallback
}
