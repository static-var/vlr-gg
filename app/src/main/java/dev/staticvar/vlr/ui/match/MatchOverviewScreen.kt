package dev.staticvar.vlr.ui.match

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.core.view.drawToBitmap
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getError
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshState
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dev.staticvar.vlr.R
import dev.staticvar.vlr.data.api.response.MatchPreviewInfo
import dev.staticvar.vlr.ui.*
import dev.staticvar.vlr.ui.common.ErrorUi
import dev.staticvar.vlr.ui.helper.CardView
import dev.staticvar.vlr.ui.helper.VLRTabIndicator
import dev.staticvar.vlr.ui.theme.VLRTheme
import dev.staticvar.vlr.utils.*
import kotlinx.coroutines.launch
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

@Composable
fun MatchOverview(viewModel: VlrViewModel) {

  val allMatches by
    remember(viewModel) { viewModel.getMatches() }.collectAsState(initial = Waiting())
  var triggerRefresh by remember(viewModel) { mutableStateOf(true) }
  val updateState by
    remember(triggerRefresh) { viewModel.refreshMatches() }.collectAsState(initial = Ok(false))

  val swipeRefresh = rememberSwipeRefreshState(isRefreshing = updateState.get() ?: false)

  val primaryContainer = VLRTheme.colorScheme.surface
  val systemUiController = rememberSystemUiController()
  SideEffect { systemUiController.setStatusBarColor(primaryContainer) }

  val modifier: Modifier = Modifier
  Column(
    modifier = modifier.fillMaxSize().statusBarsPadding(),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Box(modifier) {
      allMatches
        .onPass {
          data?.let { list ->
            MatchOverviewContainer(
              modifier,
              list = StableHolder(list),
              swipeRefresh,
              updateState,
              onClick = { id -> viewModel.action.match(id) },
              triggerRefresh = { triggerRefresh = triggerRefresh.not() }
            )
          }
        }
        .onWaiting { LinearProgressIndicator(modifier.animateContentSize()) }
        .onFail { Text(text = message()) }
    }
  }
}

const val MAX_SHARABLE_ITEMS = 6

@Composable
fun MatchOverviewContainer(
  modifier: Modifier = Modifier,
  list: StableHolder<List<MatchPreviewInfo>>,
  swipeRefresh: SwipeRefreshState,
  updateState: Result<Boolean, Throwable?>,
  onClick: (String) -> Unit,
  triggerRefresh: () -> Unit
) {
  val pagerState = rememberPagerState()
  val scope = rememberCoroutineScope()
  val shareMatchList = remember { mutableStateListOf<MatchPreviewInfo>() }
  var shareState by remember { mutableStateOf(false) }
  var shareDialog by remember { mutableStateOf(false) }
  val haptic = LocalHapticFeedback.current

  val tabs =
    listOf(
      stringResource(id = R.string.live),
      stringResource(id = R.string.upcoming),
      stringResource(id = R.string.completed)
    )

  if (shareDialog) {
    ShareDialog(matches = StableHolder(shareMatchList)) { shareDialog = false }
  }
  val mapByStatus by remember(list) { mutableStateOf(list.item.groupBy { it.status }) }

  val (ongoing, upcoming, completed) =
    remember(list) {
      mapByStatus.let {
        Triple(
          it[tabs[0].lowercase()].orEmpty(),
          it[tabs[1].lowercase()].orEmpty().sortedBy { match -> match.time?.timeToEpoch },
          it[tabs[2].lowercase()].orEmpty().sortedByDescending { match -> match.time?.timeToEpoch }
        )
      }
    }

  SwipeRefresh(state = swipeRefresh, onRefresh = triggerRefresh, indicator = { _, _ -> }) {
    Column(
      modifier = modifier.fillMaxSize().animateContentSize(),
      verticalArrangement = Arrangement.Top
    ) {
      if (updateState.get() == true || swipeRefresh.isSwipeInProgress)
        LinearProgressIndicator(
          modifier.fillMaxWidth().padding(Local16DPPadding.current).animateContentSize()
        )
      updateState.getError()?.let {
        ErrorUi(modifier = modifier, exceptionMessage = it.stackTraceToString())
      }
      AnimatedVisibility(visible = shareState) {
        SharingAppBar(
          modifier = modifier,
          items = shareMatchList,
          shareMode = {
            shareState = it
            shareMatchList.clear()
          },
          shareConfirm = { shareDialog = true }
        )
      }

      TabRow(
        selectedTabIndex = pagerState.currentPage,
        indicator = { indicators -> VLRTabIndicator(indicators, pagerState.currentPage) }
      ) {
        tabs.forEachIndexed { index, title ->
          Tab(
            selected = pagerState.currentPage == index,
            onClick = { scope.launch { pagerState.scrollToPage(index) } }
          ) { Text(text = title, modifier = modifier.padding(Local16DPPadding.current)) }
        }
      }

      HorizontalPager(count = tabs.size, state = pagerState, modifier = modifier.fillMaxSize()) {
        tabPosition ->
        when (tabPosition) {
          0 -> {
            if (ongoing.isEmpty()) {
              NoMatchUI()
            } else {
              val lazyListState = rememberLazyListState()
              LazyColumn(
                modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Top,
                state = lazyListState
              ) {
                items(ongoing, key = { item -> item.id }) {
                  MatchOverviewPreview(
                    matchPreviewInfo = it,
                    shareMode = shareState,
                    isSelected = it in shareMatchList,
                    onAction = { longPress, match ->
                      if (longPress) shareState = true // If long press enable share bar
                      when {
                        shareState && shareMatchList.contains(match) -> {
                          // If in share mode &
                          // If match is already in the list and is being clicked again, remove the
                          // item
                          shareMatchList.remove(match)
                          haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        shareState &&
                          !shareMatchList.contains(match) &&
                          shareMatchList.size < MAX_SHARABLE_ITEMS -> {
                          // If in share mode &
                          // If list does not have 6 items and if the clicked icon is not already in
                          // the list
                          shareMatchList.add(match)
                          haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        !shareState ->
                          onClick(match.id) // Its a normal click, navigate to the action
                      }
                    }
                  )
                }
              }
            }
          }
          1 -> {
            if (upcoming.isEmpty()) {
              NoMatchUI()
            } else {
              val lazyListState = rememberLazyListState()
              val groupedUpcomingMatches =
                remember(upcoming) { upcoming.groupBy { it.time?.readableDate } }
              LazyColumn(
                modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Top,
                state = lazyListState
              ) {
                groupedUpcomingMatches.forEach { (date, match)
                  -> // Group heading based on date for sticky header
                  stickyHeader {
                    date?.let {
                      Column(
                        modifier.fillMaxWidth().background(VLRTheme.colorScheme.primaryContainer)
                      ) {
                        Text(
                          it,
                          modifier = modifier.fillMaxWidth().padding(Local8DPPadding.current),
                          textAlign = TextAlign.Center,
                          color = VLRTheme.colorScheme.primary
                        )
                      }
                    }
                  }
                  items(match, key = { item -> item.id }) {
                    MatchOverviewPreview(
                      matchPreviewInfo = it,
                      shareMode = shareState,
                      isSelected = it in shareMatchList,
                      onAction = { longPress, match ->
                        if (longPress) shareState = true // If long press enable share bar
                        when {
                          shareState && shareMatchList.contains(match) -> {
                            // If in share mode &
                            // If match is already in the list and is being clicked again, remove
                            // the
                            // item
                            shareMatchList.remove(match)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                          }
                          shareState &&
                            !shareMatchList.contains(match) &&
                            shareMatchList.size < MAX_SHARABLE_ITEMS -> {
                            // If in share mode &
                            // If list does not have 6 items and if the clicked icon is not already
                            // in
                            // the list
                            shareMatchList.add(match)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                          }
                          !shareState ->
                            onClick(match.id) // Its a normal click, navigate to the action
                        }
                      }
                    )
                  }
                }
              }
            }
          }
          else -> {
            if (completed.isEmpty()) {
              NoMatchUI(modifier = modifier)
            } else {
              val lazyListState = rememberLazyListState()
              val groupedCompletedMatches =
                remember(completed) { completed.groupBy { it.time?.readableDate } }
              LazyColumn(
                modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Top,
                state = lazyListState
              ) {
                groupedCompletedMatches.forEach { (date, match)
                  -> // Group heading based on date for sticky header
                  stickyHeader {
                    date?.let {
                      Column(
                        modifier.fillMaxWidth().background(VLRTheme.colorScheme.primaryContainer),
                      ) {
                        Text(
                          it,
                          modifier = modifier.fillMaxWidth().padding(Local8DPPadding.current),
                          textAlign = TextAlign.Center,
                          color = VLRTheme.colorScheme.primary
                        )
                      }
                    }
                  }
                  items(match, key = { item -> item.id }) {
                    MatchOverviewPreview(
                      matchPreviewInfo = it,
                      shareMode = shareState,
                      isSelected = it in shareMatchList,
                      onAction = { longPress, match ->
                        if (longPress) shareState = true // If long press enable share bar
                        when {
                          shareState && shareMatchList.contains(match) -> {
                            // If in share mode &
                            // If match is already in the list and is being clicked again, remove
                            // the
                            // item
                            shareMatchList.remove(match)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                          }
                          shareState &&
                            !shareMatchList.contains(match) &&
                            shareMatchList.size < MAX_SHARABLE_ITEMS -> {
                            // If in share mode &
                            // If list does not have 6 items and if the clicked icon is not already
                            // in
                            // the list
                            shareMatchList.add(match)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                          }
                          !shareState ->
                            onClick(match.id) // Its a normal click, navigate to the action
                        }
                      }
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun NoMatchUI(modifier: Modifier = Modifier) {
  Column(modifier = modifier.fillMaxSize()) {
    Spacer(modifier = modifier.weight(1f))
    Text(
      text = stringResource(R.string.no_match),
      modifier = modifier.fillMaxWidth(),
      textAlign = TextAlign.Center,
      style = VLRTheme.typography.bodyLarge,
      color = VLRTheme.colorScheme.primary
    )
    Spacer(modifier = modifier.weight(1f))
  }
}

@Composable
fun MatchOverviewPreview(
  modifier: Modifier = Modifier,
  matchPreviewInfo: MatchPreviewInfo,
  shareMode: Boolean,
  isSelected: Boolean,
  onAction: (Boolean, MatchPreviewInfo) -> Unit,
) {
  CardView(
    modifier =
      modifier
        .pointerInput(Unit) {
          detectTapGestures(
            onPress = {},
            onDoubleTap = {},
            onLongPress = { onAction(true, matchPreviewInfo) },
            onTap = { onAction(false, matchPreviewInfo) }
          )
        }
        .apply { if (isSelected) background(VLRTheme.colorScheme.secondaryContainer) }
  ) {
    Column(modifier = modifier.padding(Local4DPPadding.current).animateContentSize()) {
      Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        Text(
          text =
            if (matchPreviewInfo.status.equals(stringResource(id = R.string.live), true))
              stringResource(id = R.string.live)
            else
              matchPreviewInfo.time?.timeDiff?.plus(" (${matchPreviewInfo.time.readableTime})")
                ?: "",
          modifier = modifier.fillMaxWidth().padding(Local8DP_4DPPadding.current),
          textAlign = TextAlign.Center,
          style = VLRTheme.typography.displaySmall
        )

        if (shareMode)
          Checkbox(checked = isSelected, onCheckedChange = { onAction(false, matchPreviewInfo) })
      }
      Row(
        modifier = modifier.padding(Local8DP_4DPPadding.current),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = matchPreviewInfo.team1.name,
          style = VLRTheme.typography.titleSmall,
          modifier = modifier.weight(1f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          color = VLRTheme.colorScheme.primary,
        )
        Text(
          text = matchPreviewInfo.team1.score?.toString() ?: "-",
          style = VLRTheme.typography.titleSmall,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          color = VLRTheme.colorScheme.primary,
        )
      }
      Row(
        modifier = modifier.padding(Local8DP_4DPPadding.current),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = matchPreviewInfo.team2.name,
          style = VLRTheme.typography.titleSmall,
          modifier = modifier.weight(1f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          color = VLRTheme.colorScheme.primary,
        )
        Text(
          text = matchPreviewInfo.team2.score?.toString() ?: "-",
          style = VLRTheme.typography.titleSmall,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          color = VLRTheme.colorScheme.primary,
        )
      }
      Text(
        text = "${matchPreviewInfo.event} - ${matchPreviewInfo.series}",
        modifier = modifier.fillMaxWidth().padding(Local8DP_4DPPadding.current),
        textAlign = TextAlign.Center,
        style = VLRTheme.typography.labelSmall
      )
    }
  }
}

@Composable
fun SharingAppBar(
  modifier: Modifier,
  items: List<MatchPreviewInfo>,
  shareMode: (Boolean) -> Unit,
  shareConfirm: (Boolean) -> Unit
) {
  Row(
    modifier.fillMaxWidth().height(40.dp).background(VLRTheme.colorScheme.background),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      imageVector = Icons.Outlined.Close,
      contentDescription = stringResource(id = R.string.cancel),
      modifier =
        modifier.padding(Local8DP_4DPPadding.current).clickable { shareMode(false) }.size(32.dp),
      tint = VLRTheme.colorScheme.primary,
    )
    Spacer(modifier = modifier.weight(1f))
    Text(
      text = "${items.size}/$MAX_SHARABLE_ITEMS",
      modifier = modifier.padding(Local8DP_4DPPadding.current),
      color = VLRTheme.colorScheme.primary
    )
    Icon(
      imageVector = Icons.Outlined.Send,
      contentDescription = stringResource(R.string.share),
      modifier =
        modifier.padding(Local8DP_4DPPadding.current).clickable { shareConfirm(true) }.size(32.dp),
      tint = VLRTheme.colorScheme.primary
    )
  }
}

@Composable
fun ShareDialog(matches: StableHolder<List<MatchPreviewInfo>>, onDismiss: () -> Unit) {
  var shareToggle by remember { mutableStateOf(false) }
  val context = LocalContext.current
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.preview), color = VLRTheme.colorScheme.primary) },
    text = {
      CaptureBitmap(
        captureRequestKey = shareToggle,
        content = { SharableListUi(matches = matches) },
        onBitmapCaptured = { bitmap ->
          val imagePath = File(context.externalCacheDir, "my_images")
          if (!imagePath.exists()) imagePath.mkdirs()
          val file = File(imagePath, System.currentTimeMillis().toString() + ".png")

          val imageUri =
            FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
          val os: OutputStream = BufferedOutputStream(FileOutputStream(file))
          os.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, os) }
          fireIntent(context = context, file = imageUri, matches = matches.item)
        }
      )
    },
    confirmButton = {
      Button(onClick = { shareToggle = shareToggle.not() }) {
        Text(text = stringResource(R.string.share))
      }
    }
  )
}

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

@Composable
fun SharableListUi(modifier: Modifier = Modifier, matches: StableHolder<List<MatchPreviewInfo>>) {
  Column(modifier.fillMaxWidth().background(VLRTheme.colorScheme.primaryContainer)) {
    CardView(
      colors =
        CardDefaults.cardColors(
          contentColor = VLRTheme.colorScheme.onPrimaryContainer,
          containerColor = VLRTheme.colorScheme.primaryContainer
        )
    ) {
      matches.item.forEachIndexed { index, matchPreviewInfo ->
        SharableMatchUi(match = matchPreviewInfo)
        if (index != matches.item.size - 1)
          Divider(modifier = Modifier.fillMaxWidth().padding(2.dp).height(0.5.dp))
      }
    }
  }
}

@Composable
fun SharableMatchUi(modifier: Modifier = Modifier, match: MatchPreviewInfo) {
  Text(
    text =
      if (match.status.equals(stringResource(R.string.live), true)) stringResource(R.string.live)
      else match.time?.readableDateAndTime ?: "",
    modifier = modifier.fillMaxWidth().padding(Local2DPPadding.current),
    textAlign = TextAlign.Center,
    style = VLRTheme.typography.labelSmall
  )
  Row(modifier = modifier.fillMaxWidth().padding(Local4DP_2DPPadding.current)) {
    Text(
      text = match.team1.name,
      overflow = TextOverflow.Ellipsis,
      maxLines = 1,
      modifier = modifier.weight(3f),
      textAlign = TextAlign.Start,
      style = VLRTheme.typography.bodySmall
    )
    Text(
      text = match.team1.score?.toString() ?: "-",
      overflow = TextOverflow.Ellipsis,
      maxLines = 1,
      modifier = modifier.weight(1f),
      textAlign = TextAlign.End,
      style = VLRTheme.typography.bodySmall
    )
  }
  Row(modifier = modifier.fillMaxWidth().padding(Local4DP_2DPPadding.current)) {
    Text(
      text = match.team2.name,
      overflow = TextOverflow.Ellipsis,
      maxLines = 1,
      modifier = modifier.weight(3f),
      textAlign = TextAlign.Start,
      style = VLRTheme.typography.bodySmall
    )
    Text(
      text = match.team2.score?.toString() ?: "-",
      overflow = TextOverflow.Ellipsis,
      maxLines = 1,
      modifier = modifier.weight(1f),
      textAlign = TextAlign.End,
      style = VLRTheme.typography.bodySmall
    )
  }
}

fun fireIntent(context: Context, file: Uri, matches: List<MatchPreviewInfo>) {
  val string = buildString {
    matches.forEach {
      appendLine(
        "${it.team1.name} vs ${it.team2.name} | ${it.time?.readableDateAndTime} | ${it.id.urlFromId()}"
      )
      appendLine()
    }
    appendLine(context.resources.getString(R.string.shared_via))
  }

  val shareIntent =
    Intent(Intent.ACTION_SEND).apply {
      flags = Intent.FLAG_GRANT_READ_URI_PERMISSION + Intent.FLAG_GRANT_WRITE_URI_PERMISSION
      putExtra(Intent.EXTRA_STREAM, file)
      putExtra(Intent.EXTRA_TEXT, string)
      type = "image/png"
    }
  context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_with)))
}

private fun String.urlFromId() = "https://vlr.gg/$this"
