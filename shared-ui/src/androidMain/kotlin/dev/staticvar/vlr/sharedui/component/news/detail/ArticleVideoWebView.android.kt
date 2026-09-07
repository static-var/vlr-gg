/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.news.detail

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Message
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.findViewTreeLifecycleOwner
import kotlin.math.abs
import kotlin.math.roundToInt

internal actual fun supportsArticleVideoWebView(): Boolean = true

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal actual fun ArticleVideoWebView(
  playerUrl: String,
  modifier: Modifier,
  contentScale: Float,
  onError: () -> Unit,
) {
  val context = LocalContext.current
  val lifecycle = LocalView.current.findViewTreeLifecycleOwner()?.lifecycle
  val currentOnError = rememberUpdatedState(onError)
  val player = remember(context, playerUrl) {
    AndroidArticlePlayer(context, playerUrl, contentScale) { currentOnError.value() }
  }

  DisposableEffect(player, lifecycle) {
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_PAUSE -> player.pause()
        Lifecycle.Event.ON_RESUME -> player.resume()
        else -> Unit
      }
    }
    lifecycle?.addObserver(observer)
    if (lifecycle == null || lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) player.resume()
    onDispose {
      lifecycle?.removeObserver(observer)
      player.release()
    }
  }

  AndroidView(
    factory = { player.webView },
    modifier = modifier,
    update = { player.updateContentScale(contentScale) },
  )
}

@SuppressLint("SetJavaScriptEnabled")
private class AndroidArticlePlayer(
  private val context: Context,
  private val playerUrl: String,
  initialContentScale: Float,
  private val onError: () -> Unit,
) {
  private var active = false
  private var released = false
  private var pageLoaded = false
  private var contentScale = initialContentScale
  private var observedScale = context.resources.displayMetrics.density * initialContentScale
  private var fullscreenDialog: Dialog? = null
  private var fullscreenCallback: WebChromeClient.CustomViewCallback? = null
  private val popupViews = mutableSetOf<WebView>()

  val webView: WebView = WebView(context).apply {
    setBackgroundColor(Color.BLACK)
    setInitialScale((observedScale * 100).roundToInt())
    settings.apply {
      javaScriptEnabled = true
      domStorageEnabled = true
      allowFileAccess = false
      allowContentAccess = false
      mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
      mediaPlaybackRequiresUserGesture = true
      useWideViewPort = true
      setSupportMultipleWindows(true)
      javaScriptCanOpenWindowsAutomatically = false
    }
    webViewClient = object : WebViewClient() {
      override fun onScaleChanged(view: WebView, oldScale: Float, newScale: Float) {
        observedScale = newScale
      }

      override fun onPageFinished(view: WebView, url: String) {
        if (active && url == playerUrl) {
          pageLoaded = true
          applyContentScale()
        }
      }

      override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        if (request.url.toString() == playerUrl) return false
        if (request.hasGesture()) {
          openExternal(request.url)
          return true
        }
        return request.isForMainFrame || request.url.scheme != "https"
      }

      override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
        if (request.isForMainFrame && active && request.url.toString() != "about:blank") onError()
      }

      override fun onReceivedHttpError(
        view: WebView,
        request: WebResourceRequest,
        errorResponse: WebResourceResponse,
      ) {
        if (request.isForMainFrame && active) onError()
      }
    }
    webChromeClient = object : WebChromeClient() {
      override fun onShowCustomView(view: View, callback: CustomViewCallback) {
        if (fullscreenDialog != null || !active) {
          callback.onCustomViewHidden()
          return
        }
        fullscreenCallback = callback
        fullscreenDialog = Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen).apply {
          setContentView(
            view,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
          )
          window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
          setOnDismissListener { closeFullscreen() }
          show()
          window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
      }

      override fun onHideCustomView() = closeFullscreen()

      override fun onCreateWindow(
        view: WebView,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: Message,
      ): Boolean {
        if (!isUserGesture || !active) return false
        val transport = resultMsg.obj as? WebView.WebViewTransport ?: return false
        val popup = WebView(context).apply {
          settings.allowFileAccess = false
          settings.allowContentAccess = false
          webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
              if (!request.isForMainFrame) return true
              openExternal(request.url)
              view.post {
                popupViews.remove(view)
                view.destroy()
              }
              return true
            }
          }
        }
        popupViews.add(popup)
        transport.webView = popup
        resultMsg.sendToTarget()
        return true
      }
    }
  }

  fun updateContentScale(value: Float) {
    if (released || contentScale == value) return
    contentScale = value
    webView.setInitialScale((context.resources.displayMetrics.density * contentScale * 100).roundToInt())
    if (pageLoaded) applyContentScale()
  }

  private fun applyContentScale() {
    val targetScale = context.resources.displayMetrics.density * contentScale
    if (observedScale > 0 && abs(targetScale - observedScale) > 0.001f) {
      webView.zoomBy((targetScale / observedScale).coerceIn(0.01f, 100f))
    }
  }

  fun resume() {
    if (released || active) return
    active = true
    pageLoaded = false
    webView.onResume()
    webView.loadUrl(playerUrl)
  }

  fun pause() {
    if (released || !active) return
    active = false
    pageLoaded = false
    closeFullscreen()
    webView.stopLoading()
    webView.loadUrl("about:blank")
    webView.onPause()
  }

  fun release() {
    if (released) return
    pause()
    released = true
    popupViews.forEach { it.destroy() }
    popupViews.clear()
    webView.webChromeClient = null
    webView.webViewClient = WebViewClient()
    (webView.parent as? ViewGroup)?.removeView(webView)
    webView.destroy()
  }

  private fun closeFullscreen() {
    val dialog = fullscreenDialog
    val callback = fullscreenCallback
    fullscreenDialog = null
    fullscreenCallback = null
    dialog?.setOnDismissListener(null)
    dialog?.dismiss()
    callback?.onCustomViewHidden()
  }

  private fun openExternal(uri: Uri) {
    if (released || !active || uri.scheme !in setOf("https", "http")) return
    try {
      context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
      onError()
    }
  }
}
