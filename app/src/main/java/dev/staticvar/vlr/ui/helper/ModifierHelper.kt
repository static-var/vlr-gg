package dev.staticvar.vlr.ui.helper

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun Modifier.takeIf(condition: Boolean, block: @Composable Modifier.() -> Modifier): Modifier {
    return if (condition) {
        block(this)
    } else {
        this
    }
}

@Composable
fun Modifier.takeIfNot(condition: Boolean, block: @Composable Modifier.() -> Modifier): Modifier {
    return if (!condition) {
        block(this)
    } else {
        this
    }
}