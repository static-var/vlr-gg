package dev.staticvar.vlr.ui.helper

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.drawToBitmap

@Composable
fun CaptureBitmap(
    captureRequestKey: Boolean,
    content: @Composable () -> Unit,
    onBitmapCaptured: (Bitmap) -> Unit
) {

    val context = LocalContext.current

    /**
     * ComposeView that would take composable as its content Kept in remember so recomposition doesn't
     * re-initialize it
     */
    val composeView = remember { ComposeView(context) }

    // If key is changed it means it's requested to capture a Bitmap
    if (captureRequestKey) composeView.post { onBitmapCaptured.invoke(composeView.drawToBitmap()) }

    /** Use Native View inside Composable */
    AndroidView(factory = { composeView.apply { setContent { content.invoke() } } })
}