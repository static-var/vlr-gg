package dev.unusedvariable.vlr.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.dropbox.android.external.store4.StoreRequest
import com.dropbox.android.external.store4.StoreResponse
import com.github.ajalt.timberkt.Timber.e
import com.google.accompanist.insets.statusBarsPadding
import dev.unusedvariable.vlr.ui.VlrViewModel
import dev.unusedvariable.vlr.ui.common.FailScreen
import dev.unusedvariable.vlr.ui.common.LoadingScreen
import dev.unusedvariable.vlr.ui.common.SuccessScreen
import dev.unusedvariable.vlr.ui.theme.VLRTheme
import dev.unusedvariable.vlr.utils.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlin.time.Duration

@Composable
fun SchedulePage(viewModel: VlrViewModel) {

    val storeInfo by remember(viewModel) {
        viewModel.getUpcomingMatches()
    }.collectAsState(initial = Waiting())

//    e { storeInfo.toString() }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .statusBarsPadding()
                .background(color = VLRTheme.colors.background.copy(0.7f))
        )
        storeInfo.onPass {
            data?.takeIf { it.isNotEmpty() }?.let {
                SuccessScreen(data = data, upcomingMatches = true, action = viewModel.action)
            } ?: FailScreen(
                info = "Unable to fetch information",
                e = IllegalStateException(),
                viewModel = viewModel,
                showReload = true
            ).also { e { data.toString() } }
        }.onFail {
            FailScreen(info = error, e = exception, viewModel = viewModel)
        }.onWaiting {
            LoadingScreen()
        }
    }
}

