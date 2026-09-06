/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package dev.staticvar.vlr.sharedui.component.news.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSError
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL
import platform.Foundation.NSURLErrorCancelled
import platform.Foundation.NSURLRequest
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationWillResignActiveNotification
import platform.WebKit.WKAudiovisualMediaTypeNone
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationAction
import platform.WebKit.WKNavigationActionPolicy
import platform.WebKit.WKNavigationActionPolicy.WKNavigationActionPolicyAllow
import platform.WebKit.WKNavigationActionPolicy.WKNavigationActionPolicyCancel
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKNavigationResponse
import platform.WebKit.WKNavigationResponsePolicy
import platform.WebKit.WKNavigationResponsePolicy.WKNavigationResponsePolicyAllow
import platform.WebKit.WKNavigationResponsePolicy.WKNavigationResponsePolicyCancel
import platform.WebKit.WKNavigationTypeLinkActivated
import platform.WebKit.WKUIDelegateProtocol
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.WebKit.WKWindowFeatures
import platform.darwin.NSObject

internal actual fun supportsArticleVideoWebView(): Boolean = true

@Composable
internal actual fun ArticleVideoWebView(playerUrl: String, modifier: Modifier, onError: () -> Unit) {
  val currentOnError = rememberUpdatedState(onError)
  val delegate = remember(playerUrl) { ArticlePlayerDelegate(playerUrl) { currentOnError.value() } }
  val webView = remember(playerUrl) {
    val configuration = WKWebViewConfiguration().apply {
      allowsInlineMediaPlayback = true
      allowsPictureInPictureMediaPlayback = false
      mediaTypesRequiringUserActionForPlayback = WKAudiovisualMediaTypeNone
    }
    WKWebView(frame = CGRectZero.readValue(), configuration = configuration).apply {
      navigationDelegate = delegate
      UIDelegate = delegate
      scrollView.scrollEnabled = false
      allowsBackForwardNavigationGestures = false
    }
  }
  DisposableEffect(webView) {
    val observer = NSNotificationCenter.defaultCenter.addObserverForName(
      name = UIApplicationWillResignActiveNotification,
      `object` = null,
      queue = NSOperationQueue.mainQueue,
    ) { webView.pauseAllMediaPlaybackWithCompletionHandler(null) }
    val url = NSURL.URLWithString(playerUrl)
    if (url?.scheme == "https") {
      webView.loadRequest(NSURLRequest.requestWithURL(url))
    } else {
      currentOnError.value()
    }
    onDispose {
      NSNotificationCenter.defaultCenter.removeObserver(observer)
      webView.navigationDelegate = null
      webView.UIDelegate = null
      webView.pauseAllMediaPlaybackWithCompletionHandler(null)
      webView.stopLoading()
      webView.loadHTMLString("", baseURL = null)
    }
  }
  key(playerUrl) {
    UIKitView(
      factory = { webView },
      modifier = modifier,
      properties = UIKitInteropProperties(isNativeAccessibilityEnabled = true),
    )
  }
}

private class ArticlePlayerDelegate(private val playerUrl: String, private val onError: () -> Unit) :
  NSObject(),
  WKNavigationDelegateProtocol,
  WKUIDelegateProtocol {
  @ObjCSignatureOverride
  override fun webView(
    webView: WKWebView,
    decidePolicyForNavigationAction: WKNavigationAction,
    decisionHandler: (WKNavigationActionPolicy) -> Unit,
  ) {
    val action = decidePolicyForNavigationAction
    val url = action.request.URL
    val isWebUrl = url?.scheme == "https" || url?.scheme == "http"
    if (action.navigationType == WKNavigationTypeLinkActivated && isWebUrl && url != null) {
      UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any>(), completionHandler = null)
      decisionHandler(WKNavigationActionPolicyCancel)
    } else if (action.targetFrame?.mainFrame == false && (isWebUrl || url?.absoluteString == "about:blank")) {
      decisionHandler(WKNavigationActionPolicyAllow)
    } else if (url?.absoluteString == playerUrl) {
      decisionHandler(WKNavigationActionPolicyAllow)
    } else {
      decisionHandler(WKNavigationActionPolicyCancel)
    }
  }

  @ObjCSignatureOverride
  override fun webView(
    webView: WKWebView,
    decidePolicyForNavigationResponse: WKNavigationResponse,
    decisionHandler: (WKNavigationResponsePolicy) -> Unit,
  ) {
    val response = decidePolicyForNavigationResponse
    val statusCode = (response.response as? NSHTTPURLResponse)?.statusCode ?: 200
    if (response.forMainFrame && statusCode >= 400) {
      decisionHandler(WKNavigationResponsePolicyCancel)
      onError()
    } else {
      decisionHandler(WKNavigationResponsePolicyAllow)
    }
  }

  @ObjCSignatureOverride
  override fun webView(webView: WKWebView, didFailNavigation: WKNavigation?, withError: NSError) {
    if (withError.code != NSURLErrorCancelled) onError()
  }

  @ObjCSignatureOverride
  override fun webView(webView: WKWebView, didFailProvisionalNavigation: WKNavigation?, withError: NSError) {
    if (withError.code != NSURLErrorCancelled) onError()
  }

  override fun webViewWebContentProcessDidTerminate(webView: WKWebView) {
    onError()
  }

  override fun webView(
    webView: WKWebView,
    createWebViewWithConfiguration: WKWebViewConfiguration,
    forNavigationAction: WKNavigationAction,
    windowFeatures: WKWindowFeatures,
  ): WKWebView? {
    val url = forNavigationAction.request.URL
    if (
      forNavigationAction.navigationType == WKNavigationTypeLinkActivated &&
      url != null && (url.scheme == "https" || url.scheme == "http")
    ) {
      UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any>(), completionHandler = null)
    }
    return null
  }
}
