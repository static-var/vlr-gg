package dev.staticvar.vlr.shared

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Main entry point for the shared Compose UI.
 * This will be used by both Android and iOS platforms.
 */
@Composable
fun App() {
  MaterialTheme {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Text(
          text = "Hello World",
          style = MaterialTheme.typography.headlineLarge,
          fontWeight = FontWeight.Bold
        )
      }
    }
  }
}
