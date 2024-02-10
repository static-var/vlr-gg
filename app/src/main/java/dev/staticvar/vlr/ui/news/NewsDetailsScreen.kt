package dev.staticvar.vlr.ui.news

import android.os.Build
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewClientCompat
import androidx.webkit.WebViewFeature
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getError
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewState
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.staticvar.vlr.data.Heading
import dev.staticvar.vlr.data.ListItem
import dev.staticvar.vlr.data.Paragraph
import dev.staticvar.vlr.data.Quote
import dev.staticvar.vlr.data.Subtext
import dev.staticvar.vlr.data.Tweet
import dev.staticvar.vlr.data.Unknown
import dev.staticvar.vlr.data.Video
import dev.staticvar.vlr.ui.Local8DPPadding
import dev.staticvar.vlr.ui.VlrViewModel
import dev.staticvar.vlr.ui.scrim.NavigationBarSpacer
import dev.staticvar.vlr.ui.scrim.NavigationBarType
import dev.staticvar.vlr.ui.scrim.StatusBarSpacer
import dev.staticvar.vlr.ui.scrim.StatusBarType
import dev.staticvar.vlr.ui.theme.VLRTheme
import dev.staticvar.vlr.utils.openAsCustomTab
import kotlinx.coroutines.launch

@Composable
fun NewsDetailsScreen(viewModel: VlrViewModel, id: String) {
  val parsedNews by remember { viewModel.parseNews(id) }.collectAsStateWithLifecycle()
  val modifier = Modifier
  val isDarkMode = isSystemInDarkTheme()
  val context = LocalContext.current
  val hazeState = remember { HazeState() }
  val scrollState = rememberLazyListState()


  Box(
    modifier = modifier.fillMaxSize(),
  ) {
    parsedNews?.get()?.let { news ->
      LazyColumn(
        modifier = modifier
          .fillMaxSize()
          .padding(horizontal = 8.dp)
          .testTag("news:root")
          .haze(hazeState),
        state = scrollState
      ) {
        item { StatusBarSpacer(statusBarType = StatusBarType.TRANSPARENT) }
        item {
          Text(
            text = news.title,
            style = VLRTheme.typography.headlineMedium,
            color = VLRTheme.colorScheme.primary
          )
        }
        item {
          Text(
            text = news.authorName,
            style = VLRTheme.typography.labelLarge,
            color = VLRTheme.colorScheme.primary
          )
        }
        item {
          Text(
            text = news.time,
            style = VLRTheme.typography.labelLarge,
            color = VLRTheme.colorScheme.primary
          )
        }
        item { Spacer(modifier = modifier.padding(Local8DPPadding.current)) }

        news.news?.let {
          items(it) { parsedData ->
            when (parsedData) {
              is Heading ->
                Text(
                  text = parsedData.text,
                  style = VLRTheme.typography.titleMedium,
                  color = VLRTheme.colorScheme.primary,
                  modifier = modifier.padding(vertical = 8.dp)
                )

              is ListItem ->
                Row(modifier = modifier.fillMaxWidth()) {
                  Text(text = " - ", color = VLRTheme.colorScheme.primary)
                  Text(text = parsedData.text, modifier = modifier.padding(vertical = 2.dp))
                }

              is Paragraph ->
                Text(text = parsedData.text, modifier = modifier.padding(vertical = 4.dp))

              is Subtext ->
                Text(
                  text = parsedData.text,
                  style = VLRTheme.typography.labelMedium,
                  modifier = modifier
                    .fillMaxSize()
                    .padding(vertical = 2.dp),
                  textAlign = TextAlign.Center
                )

              is Tweet ->
                WebView(
                  state = rememberWebViewState(""),
                  modifier = modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                  onCreated = { webView ->
                    webView.settings.javaScriptEnabled = true
                    webView.settings.domStorageEnabled = true
                    if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK) && WebViewFeature.isFeatureSupported(
                        WebViewFeature.ALGORITHMIC_DARKENING
                      ) && isDarkMode && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                    ) {
                      WebSettingsCompat.setAlgorithmicDarkeningAllowed(webView.settings, true)
                    }
                    webView.loadDataWithBaseURL(
                      null,
                      parsedData.tweetUrl,
                      "text/html",
                      "UTF-8",
                      null
                    )
                    webView.settings.setSupportMultipleWindows(true)
                    webView.settings.javaScriptCanOpenWindowsAutomatically = true
                    webView.webViewClient =
                      object : WebViewClientCompat() {
                        override fun shouldOverrideUrlLoading(
                          view: WebView,
                          request: WebResourceRequest,
                        ): Boolean {
                          println(request.url)
                          request.url?.toString()?.openAsCustomTab(context)
                          return true
                        }
                      }
                  }
                )

              is Unknown ->
                Text(
                  text = parsedData.text,
                  style = VLRTheme.typography.labelMedium,
                  modifier = modifier.padding(vertical = 2.dp)
                )

              is Video ->
                WebView(
                  state = rememberWebViewState(url = parsedData.link),
                  modifier = modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .aspectRatio(1.6f),
                  onCreated = { webView -> webView.settings.javaScriptEnabled = true }
                )

              is Quote ->
                Row(
                  modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max)
                ) {
                  Spacer(
                    modifier =
                    modifier
                      .width(12.dp)
                      .padding(4.dp)
                      .background(VLRTheme.colorScheme.primary)
                      .fillMaxHeight()
                  )
                  Text(
                    text = parsedData.text,
                    style = VLRTheme.typography.bodyMedium,
                    modifier = modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                  )
                }
            }
          }
        }

        item { NavigationBarSpacer(navigationBarType = NavigationBarType.TRANSPARENT) }
      }

      ScrollToTopButton(scrollState = scrollState, hazeState = hazeState)
    }

      ?: parsedNews?.getError()?.let {
        Text(
          text = it.stackTraceToString(),
          modifier = Modifier.align(
            Alignment.Center
          )
        )
      }
      ?: LinearProgressIndicator(
        modifier
          .testTag("common:loader")
          .align(Alignment.Center)
      )
  }
}


@Composable
fun BoxScope.ScrollToTopButton(
  modifier: Modifier = Modifier,
  scrollState: LazyListState,
  hazeState: HazeState,
) {

  val showScrollToTop by remember {
    derivedStateOf { scrollState.firstVisibleItemIndex > 0 }
  }

  val coroutineScope = rememberCoroutineScope()
  AnimatedVisibility(
    visible = showScrollToTop,
    modifier = modifier
      .align(Alignment.BottomEnd)
      .navigationBarsPadding()
      .padding(16.dp)
  ) {
    Box(
      modifier = Modifier
        .background(Color.Transparent)
        .hazeChild(
          hazeState,
          shape = VLRTheme.shapes.large,
          style = HazeMaterials.ultraThin()
        )
        .clip(VLRTheme.shapes.large)
        .border(1.dp, VLRTheme.colorScheme.primary, VLRTheme.shapes.large)
        .clickable {
          coroutineScope.launch {
            scrollState.animateScrollToItem(0)
          }
        },
    ) {
      Icon(
        imageVector = Icons.Outlined.KeyboardArrowUp,
        contentDescription = "Scroll to Top",
        modifier = Modifier
          .background(Color.Transparent)
          .padding(6.dp),
        tint = VLRTheme.colorScheme.primary,
      )
    }
  }

}
