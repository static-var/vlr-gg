/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.share

import android.content.ClipData
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
public fun rememberAndroidImageSharer(): ImageSharer {
  val context = LocalContext.current
  return remember(context) {
    object : ImageSharer {
      override suspend fun share(image: ImageBitmap, text: String) {
        val uri = withContext(Dispatchers.IO) {
          val directory = File(context.cacheDir, "match-images").apply {
            check(isDirectory || mkdirs()) { "Unable to create the image sharing directory." }
          }
          val file = File.createTempFile("vlr-matches-", ".png", directory)
          file.outputStream().use { output ->
            check(image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, output)) {
              "Unable to prepare the match image."
            }
          }
          FileProvider.getUriForFile(context, "${context.packageName}.calendar", file)
        }
        withContext(Dispatchers.Main) {
          val share = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, text)
            clipData = ClipData.newRawUri("Match overview", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
          }
          context.startActivity(Intent.createChooser(share, "Share matches"))
        }
      }
    }
  }
}
