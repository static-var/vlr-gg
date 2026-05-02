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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import dev.staticvar.designsystem.component.card.PrismCard
import dev.staticvar.designsystem.component.card.PrismCardVariant
import dev.staticvar.designsystem.preview.PrismPreview
import dev.staticvar.designsystem.preview.PrismPreviewProvider
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.designsystem.prism.PrismTheme
import dev.staticvar.designsystem.prism.PrismVariant

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
  val cosmicGradient = planetGradient(planet.id)

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

private fun planetGradient(planetId: Int): Brush {
  val gradientDirection = planetGradientDirection(planetId)
  return Brush.linearGradient(
    colors = planetColors(planetId),
    start = gradientDirection.first,
    end = gradientDirection.second,
  )
}

private fun planetGradientDirection(planetId: Int): Pair<Offset, Offset> =
  when (planetId % 4) {
    0 -> Offset.Zero to Offset.Infinite
    1 -> Offset.Infinite to Offset.Zero
    2 -> Offset(0f, Float.POSITIVE_INFINITY) to Offset(Float.POSITIVE_INFINITY, 0f)
    else -> Offset(Float.POSITIVE_INFINITY, 0f) to Offset(0f, Float.POSITIVE_INFINITY)
  }

private fun planetColors(planetId: Int): List<Color> =
  when (planetId) {
    1 -> listOf(Color(0xFF2D1B4E), Color(0xFF6B46C1), Color(0xFFDB7C26), Color(0xFFFFB84D))
    2 -> listOf(Color(0xFF1A0A0A), Color(0xFF4A1010), Color(0xFFB91C1C), Color(0xFFDC6B4A))
    3 -> listOf(Color(0xFF3D2817), Color(0xFF92400E), Color(0xFFEA580C), Color(0xFFF59E0B))
    4 -> listOf(Color(0xFF312716), Color(0xFF78350F), Color(0xFFD97706), Color(0xFFFBBF24))
    5 -> listOf(Color(0xFF0C1844), Color(0xFF1E3A8A), Color(0xFF1D4ED8), Color(0xFF3B82F6))
    else -> listOf(Color(0xFF0A2540), Color(0xFF0F4C75), Color(0xFF0891B2), Color(0xFF10B981))
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
