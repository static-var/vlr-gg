@file:Suppress("LongParameterList", "MatchingDeclarationName")

package dev.staticvar.designsystem.component.carousel

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.staticvar.designsystem.component.surface.PrismSurface
import dev.staticvar.designsystem.prism.Prism
import kotlin.math.absoluteValue

/**
 * Carousel size variants for the Prism design system.
 *
 * - **Hero**: full-width, single prominent item
 * - **Multibrowse**: center-focused item with side peeks
 * - **Uncontained**: compact rail showing multiple items
 */
public enum class PrismCarouselVariant {
  Hero,
  Multibrowse,
  Uncontained,
}

private data class CarouselConfig(
  val pageSize: PageSize,
  val contentPadding: PaddingValues,
)

private object CarouselConstants {
  const val MultibrowseWidthFraction: Float = 0.85f
  val MultibrowseMinWidth: Dp = 200.dp
  val UncontainedPageWidth: Dp = 140.dp
  val IndicatorInactiveWidth: Dp = 20.dp
  val IndicatorActiveWidth: Dp = 44.dp
  val IndicatorHeight: Dp = 8.dp
  const val IndicatorOffsetThreshold: Float = 0.5f
  const val AnimationDurationMillis: Int = 300
  const val SnapMaxAdvancePages: Int = 1
}

@Stable
private data class CarouselTokens(
  val spacingM: Dp,
  val spacingS: Dp,
  val uncontainedContentPadding: PaddingValues,
)

@Composable
private fun rememberCarouselTokens(): CarouselTokens {
  val spacingM = Prism.dimens.spacingM
  val spacingS = Prism.dimens.spacingS
  return remember(spacingM, spacingS) {
    CarouselTokens(
      spacingM = spacingM,
      spacingS = spacingS,
      uncontainedContentPadding = PaddingValues(horizontal = spacingM),
    )
  }
}

@Stable
private data class PagerPosition(
  val page: Int,
  val offsetFraction: Float,
)

@Composable
private fun rememberPagerPosition(pagerState: PagerState): State<PagerPosition> =
  remember(pagerState) {
    derivedStateOf { PagerPosition(pagerState.currentPage, pagerState.currentPageOffsetFraction) }
  }


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun rememberCarouselConfig(
  variant: PrismCarouselVariant,
  maxWidth: Dp,
  density: Density,
  tokens: CarouselTokens,
): CarouselConfig =
  remember(variant, maxWidth, density, tokens) {
    when (variant) {
      PrismCarouselVariant.Hero ->
        CarouselConfig(
          pageSize = PageSize.Fill,
          contentPadding = PaddingValues(0.dp),
        )

      PrismCarouselVariant.Multibrowse -> {
        val containerWidthPx = with(density) { maxWidth.toPx() }
        val targetWidthPx =
          (containerWidthPx * CarouselConstants.MultibrowseWidthFraction)
            .coerceAtLeast(with(density) { CarouselConstants.MultibrowseMinWidth.toPx() })
        val horizontalPaddingPx = ((containerWidthPx - targetWidthPx) / 2f).coerceAtLeast(0f)

        CarouselConfig(
          pageSize = PageSize.Fixed(with(density) { targetWidthPx.toDp() }),
          contentPadding = PaddingValues(horizontal = with(density) { horizontalPaddingPx.toDp() }),
        )
      }

      PrismCarouselVariant.Uncontained ->
        CarouselConfig(
          pageSize = PageSize.Fixed(CarouselConstants.UncontainedPageWidth),
          contentPadding = tokens.uncontainedContentPadding,
        )
    }
  }

/**
 * Prism design system carousel component.
 *
 * Flat brutalist carousel built on Foundation HorizontalPager.
 *
 * Example usage:
 * ```
 * PrismCarousel(
 *   itemCount = 5,
 *   variant = PrismCarouselVariant.Multibrowse,
 * ) { page ->
 *   Text("Page $page")
 * }
 * ```
 *
 * @param itemCount Number of items in the carousel
 * @param modifier Modifier to apply to the carousel container
 * @param variant Size variant of the carousel
 * @param pagerState State of the pager (for external control)
 * @param pageSpacing Spacing between pages
 * @param showIndicators Whether to show page indicators
 * @param indicatorColor Color of inactive indicators
 * @param activeIndicatorColor Color of active indicator
 * @param pageContent Composable for each page
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
public fun PrismCarousel(
  itemCount: Int,
  modifier: Modifier = Modifier,
  variant: PrismCarouselVariant = PrismCarouselVariant.Hero,
  pagerState: PagerState = rememberPagerState(pageCount = { itemCount }),
  pageSpacing: Dp = Prism.dimens.spacingM,
  showIndicators: Boolean = true,
  indicatorColor: Color = Prism.color.stroke,
  activeIndicatorColor: Color = Prism.color.accent,
  pageContent: @Composable (page: Int) -> Unit,
) {
  val tokens = rememberCarouselTokens()

  Column(modifier = modifier) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
      val density = LocalDensity.current
      val config = rememberCarouselConfig(
        variant = variant,
        maxWidth = maxWidth,
        density = density,
        tokens = tokens,
      )
      val nativeSnapFlingBehavior =
        PagerDefaults.flingBehavior(
          state = pagerState,
          pagerSnapDistance = PagerSnapDistance.atMost(CarouselConstants.SnapMaxAdvancePages),
          snapAnimationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        )
      HorizontalPager(
        state = pagerState,
        pageSize = config.pageSize,
        pageSpacing = pageSpacing,
        contentPadding = config.contentPadding,
        flingBehavior = nativeSnapFlingBehavior,
        beyondViewportPageCount = 2,
        modifier = Modifier.fillMaxWidth(),
      ) { page ->
        pageContent(page)
      }
    }

    val shouldShowIndicators =
      showIndicators && itemCount > 1 && variant != PrismCarouselVariant.Uncontained

    if (shouldShowIndicators) {
      CarouselIndicators(
        itemCount = itemCount,
        pagerState = pagerState,
        indicatorColor = indicatorColor,
        activeIndicatorColor = activeIndicatorColor,
        tokens = tokens,
        modifier = Modifier.fillMaxWidth().padding(top = tokens.spacingM),
      )
    }
  }
}

/**
 * Carousel page indicators with smooth animations.
 *
 * Animates size, color, and alpha based on page position.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CarouselIndicators(
  itemCount: Int,
  pagerState: PagerState,
  indicatorColor: Color,
  activeIndicatorColor: Color,
  tokens: CarouselTokens,
  modifier: Modifier = Modifier,
) {
  val pagerPosition by rememberPagerPosition(pagerState)
  val dpAnimationSpec = remember { tween<Dp>(durationMillis = CarouselConstants.AnimationDurationMillis) }
  val colorAnimationSpec = remember { tween<Color>(durationMillis = CarouselConstants.AnimationDurationMillis) }

  Row(
    modifier = modifier.wrapContentWidth(Alignment.CenterHorizontally),
    horizontalArrangement =
      Arrangement.spacedBy(tokens.spacingS, Alignment.CenterHorizontally),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    repeat(itemCount) { index ->
      val pageOffset = ((pagerPosition.page - index) + pagerPosition.offsetFraction).absoluteValue
      val isActive = pageOffset < CarouselConstants.IndicatorOffsetThreshold
      val indicatorWidth by
        animateDpAsState(
          targetValue =
            if (isActive) CarouselConstants.IndicatorActiveWidth
            else CarouselConstants.IndicatorInactiveWidth,
          animationSpec = dpAnimationSpec,
          label = "indicator_width_$index",
        )

      val indicatorContainerColor by
        animateColorAsState(
          targetValue = if (isActive) activeIndicatorColor.copy(alpha = 0.18f) else Prism.color.surface,
          animationSpec = colorAnimationSpec,
          label = "indicator_container_color_$index",
        )
      val indicatorBorderColor by
        animateColorAsState(
          targetValue = if (isActive) activeIndicatorColor else indicatorColor,
          animationSpec = colorAnimationSpec,
          label = "indicator_border_color_$index",
        )

      PrismSurface(
        modifier = Modifier.width(indicatorWidth).height(CarouselConstants.IndicatorHeight),
        color = indicatorContainerColor,
        shape = Prism.shapes.small,
        border = BorderStroke(Prism.dimens.strokeDefault, indicatorBorderColor),
      ) {}
    }
  }
}
