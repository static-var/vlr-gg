package dev.unusedvariable.vlr.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.github.ajalt.timberkt.Timber.e
import com.google.accompanist.insets.statusBarsPadding
import dev.unusedvariable.vlr.ui.VlrViewModel
import dev.unusedvariable.vlr.ui.common.FailScreen
import dev.unusedvariable.vlr.ui.common.LoadingScreen
import dev.unusedvariable.vlr.ui.common.SuccessScreen
import dev.unusedvariable.vlr.ui.theme.VLRTheme
import dev.unusedvariable.vlr.utils.Waiting
import dev.unusedvariable.vlr.utils.onFail
import dev.unusedvariable.vlr.utils.onPass
import dev.unusedvariable.vlr.utils.onWaiting

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

