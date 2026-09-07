/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
@file:OptIn(
  kotlinx.cinterop.ExperimentalForeignApi::class,
  kotlinx.cinterop.BetaInteropApi::class,
)

package dev.staticvar.vlr.sharedui.share

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.uikit.LocalUIViewController
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import platform.Foundation.NSData
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.Foundation.create
import platform.Foundation.writeToURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIWindow
import platform.UIKit.popoverPresentationController
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Composable
public fun rememberIosImageSharer(): ImageSharer {
  val controller = LocalUIViewController.current
  return remember(controller) {
    object : ImageSharer {
      override suspend fun share(image: ImageBitmap, text: String) {
        val bytes = withContext(Dispatchers.Default) {
          val skiaImage = Image.makeFromBitmap(image.asSkiaBitmap())
          try {
            val encoded = checkNotNull(skiaImage.encodeToData(EncodedImageFormat.PNG)) {
              "Unable to prepare the match image."
            }
            try {
              encoded.bytes
            } finally {
              encoded.close()
            }
          } finally {
            skiaImage.close()
          }
        }
        val fileUrl = withContext(Dispatchers.Default) {
          val url = NSURL.fileURLWithPath(NSTemporaryDirectory() + "vlr-matches-${NSUUID().UUIDString}.png")
          val data = bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()) }
          check(data.writeToURL(url, atomically = true)) { "Unable to save the match image for sharing." }
          url
        }
        withContext(Dispatchers.Main) {
          val hostWindow = checkNotNull(controller.view.window) {
            "The match screen is no longer available for sharing."
          }
          val activeWindow = hostWindow.windowScene?.windows
            ?.filterIsInstance<UIWindow>()
            ?.firstOrNull { it.isKeyWindow() }
            ?: hostWindow
          var presenter = activeWindow.rootViewController ?: controller
          while (presenter.presentedViewController != null) presenter = presenter.presentedViewController!!
          check(presenter.view.window != null && !presenter.isBeingDismissed()) {
            "The match screen is no longer available for sharing."
          }
          val activity = UIActivityViewController(activityItems = listOf(fileUrl, text), applicationActivities = null)
          activity.popoverPresentationController?.apply {
            sourceView = presenter.view
            sourceRect = presenter.view.bounds
            permittedArrowDirections = 0uL
          }
          suspendCancellableCoroutine<Unit> { continuation ->
            activity.completionWithItemsHandler = { _, _, _, error ->
              if (continuation.isActive) {
                if (error == null) {
                  continuation.resume(Unit)
                } else {
                  continuation.resumeWithException(IllegalStateException(error.localizedDescription))
                }
              }
            }
            continuation.invokeOnCancellation {
              NSOperationQueue.mainQueue.addOperationWithBlock {
                if (activity.presentingViewController != null) {
                  activity.dismissViewControllerAnimated(true, completion = null)
                }
              }
            }
            presenter.presentViewController(activity, animated = true, completion = null)
          }
        }
      }
    }
  }
}
