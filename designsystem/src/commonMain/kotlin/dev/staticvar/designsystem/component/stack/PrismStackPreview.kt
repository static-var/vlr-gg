package dev.staticvar.designsystem.component.stack

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.staticvar.designsystem.component.card.PrismCard
import dev.staticvar.designsystem.component.card.PrismCardVariant
import dev.staticvar.designsystem.preview.PrismPreview
import dev.staticvar.designsystem.preview.PrismPreviewProvider
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.designsystem.prism.PrismTheme
import dev.staticvar.designsystem.prism.PrismVariant
import androidx.compose.ui.tooling.preview.PreviewParameter

@PrismPreview
@Composable
internal fun PrismStackPreview(
  @PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant,
) {
  PrismTheme(variant = variant) {
    val planets = remember { mutableStateOf(sampleStackPlanets) }
    Box(
      modifier =
        Modifier.fillMaxSize()
          .background(Prism.color.background)
          .padding(Prism.dimens.spacingL),
      contentAlignment = Alignment.BottomCenter,
    ) {
      PrismStack(
        modifier = Modifier.fillMaxWidth(),
        list = planets.value,
        onSwipe = { swiped -> planets.value = planets.value.drop(1) + swiped },
        shape = Prism.shapes.large,
      ) { planet ->
        PrismStackPreviewCard(planet = planet)
      }
    }
  }
}

@Composable
private fun PrismStackPreviewCard(planet: StackPreviewPlanet) {
  val planetColors = when (planet.id) {
    1 -> listOf(
      Color(0xFF2D1B4E), // Deep purple (Venus night)
      Color(0xFF6B46C1), // Purple
      Color(0xFFDB7C26), // Amber/orange (sulfuric clouds)
      Color(0xFFFFB84D), // Golden yellow
    )
    2 -> listOf(
      Color(0xFF1A0A0A), // Deep black
      Color(0xFF4A1010), // Dark red
      Color(0xFFB91C1C), // Mars red
      Color(0xFFDC6B4A), // Rust orange
    )
    3 -> listOf(
      Color(0xFF3D2817), // Dark brown
      Color(0xFF92400E), // Brown
      Color(0xFFEA580C), // Orange (Great Red Spot)
      Color(0xFFF59E0B), // Amber
    )
    4 -> listOf(
      Color(0xFF312716), // Deep brown
      Color(0xFF78350F), // Golden brown
      Color(0xFFD97706), // Amber gold
      Color(0xFFFBBF24), // Light gold (rings)
    )
    5 -> listOf(
      Color(0xFF0C1844), // Deep space blue
      Color(0xFF1E3A8A), // Dark blue
      Color(0xFF1D4ED8), // Neptune blue
      Color(0xFF3B82F6), // Bright blue
    )
    else -> listOf(
      Color(0xFF0A2540), // Deep ocean blue
      Color(0xFF0F4C75), // Ocean blue
      Color(0xFF0891B2), // Cyan (water)
      Color(0xFF10B981), // Green (land)
    )
  }

  val gradientDirection = when (planet.id % 4) {
    0 -> androidx.compose.ui.geometry.Offset(0f, 0f) to androidx.compose.ui.geometry.Offset.Infinite
    1 -> androidx.compose.ui.geometry.Offset.Infinite to androidx.compose.ui.geometry.Offset(0f, 0f)
    2 -> androidx.compose.ui.geometry.Offset(0f, Float.POSITIVE_INFINITY) to androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, 0f)
    else -> androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, 0f) to androidx.compose.ui.geometry.Offset(0f, Float.POSITIVE_INFINITY)
  }

  val cosmicGradient = Brush.linearGradient(
    colors = planetColors,
    start = gradientDirection.first,
    end = gradientDirection.second,
  )

  PrismCard(
    modifier = Modifier.fillMaxWidth().height(220.dp),
    variant = PrismCardVariant.Gradient,
    brush = cosmicGradient,
    shape = Prism.shapes.large,
    contentColor = Color.White,
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(Prism.dimens.spacingL),
      verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
    ) {
      Text(
        text = planet.name,
        style = Prism.typography.headline,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        color = Color.White,
      )
      Spacer(modifier = Modifier.height(Prism.dimens.spacingXs))
      Text(
        text = planet.description,
        style = Prism.typography.bodyLarge,
        maxLines = 4,
        overflow = TextOverflow.Ellipsis,
        color = Color.White.copy(alpha = 0.85f),
      )
    }
  }
}

private data class StackPreviewPlanet(val id: Int, val name: String, val description: String)

private val sampleStackPlanets =
  listOf(
    StackPreviewPlanet(1, "Venus", "Second planet from the sun, wrapped in dense sulfuric clouds."),
    StackPreviewPlanet(2, "Mars", "Cold desert world with iron oxide sands giving a red hue."),
    StackPreviewPlanet(3, "Jupiter", "Gas giant featuring the Great Red Spot storm."),
    StackPreviewPlanet(4, "Saturn", "Iconic ring system composed of ice and rocky debris."),
    StackPreviewPlanet(5, "Neptune", "Blue ice giant with supersonic winds and deep storms."),
    StackPreviewPlanet(6, "Earth", "Water-rich world supporting a complex biosphere."),
  )
