/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
@file:Suppress("LongParameterList", "MatchingDeclarationName")

package dev.staticvar.designsystem.component.carousel

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

private object CarouselConstants {
  val MultibrowsePeek: Dp = 30.dp
  val UncontainedPageSize: PageSize = PageSize.Fixed(140.dp)
  val IndicatorInactiveWidth: Dp = 20.dp
  val IndicatorActiveWidth: Dp = 44.dp
  val IndicatorHeight: Dp = 8.dp
  const val IndicatorOffsetThreshold: Float = 0.5f
  const val AnimationDurationMillis: Int = 300
  const val SnapMaxAdvancePages: Int = 1
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
  key: ((page: Int) -> Any)? = null,
  pageContent: @Composable (page: Int) -> Unit,
) {
  val spacingM = Prism.dimens.spacingM
  val contentPadding = remember(variant, spacingM) {
    when (variant) {
      PrismCarouselVariant.Hero -> PaddingValues(0.dp)
      PrismCarouselVariant.Multibrowse -> PaddingValues(horizontal = CarouselConstants.MultibrowsePeek)
      PrismCarouselVariant.Uncontained -> PaddingValues(horizontal = spacingM)
    }
  }
  val pageSize = if (variant == PrismCarouselVariant.Uncontained) {
    CarouselConstants.UncontainedPageSize
  } else {
    PageSize.Fill
  }

  Column(modifier = modifier) {
    CarouselPager(
      pagerState = pagerState,
      pageSize = pageSize,
      contentPadding = contentPadding,
      pageSpacing = pageSpacing,
      key = key,
      pageContent = pageContent,
    )

    if (showIndicators && itemCount > 1 && variant != PrismCarouselVariant.Uncontained) {
      CarouselIndicators(
        itemCount = itemCount,
        pagerState = pagerState,
        indicatorColor = indicatorColor,
        activeIndicatorColor = activeIndicatorColor,
        modifier = Modifier.fillMaxWidth().padding(top = spacingM),
      )
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CarouselPager(
  pagerState: PagerState,
  pageSize: PageSize,
  contentPadding: PaddingValues,
  pageSpacing: Dp,
  key: ((page: Int) -> Any)?,
  pageContent: @Composable (page: Int) -> Unit,
) {
  HorizontalPager(
    state = pagerState,
    key = key,
    pageSize = pageSize,
    pageSpacing = pageSpacing,
    contentPadding = contentPadding,
    flingBehavior = PagerDefaults.flingBehavior(
      state = pagerState,
      pagerSnapDistance = PagerSnapDistance.atMost(CarouselConstants.SnapMaxAdvancePages),
      snapAnimationSpec = spring(stiffness = Spring.StiffnessMediumLow),
    ),
    beyondViewportPageCount = 2,
    modifier = Modifier.fillMaxWidth(),
  ) { page ->
    pageContent(page)
  }
}

/**
 * Carousel page indicators with smooth animations.
 *
 * Animates size and color when the settled page changes.
 */
@Composable
private fun CarouselIndicators(
  itemCount: Int,
  pagerState: PagerState,
  indicatorColor: Color,
  activeIndicatorColor: Color,
  modifier: Modifier = Modifier,
) {
  val activePage by remember(pagerState) {
    derivedStateOf {
      if (pagerState.currentPageOffsetFraction.absoluteValue < CarouselConstants.IndicatorOffsetThreshold) {
        pagerState.currentPage
      } else {
        -1
      }
    }
  }
  val dpAnimationSpec = remember { tween<Dp>(durationMillis = CarouselConstants.AnimationDurationMillis) }
  val colorAnimationSpec = remember { tween<Color>(durationMillis = CarouselConstants.AnimationDurationMillis) }

  Row(
    modifier = modifier.wrapContentWidth(Alignment.CenterHorizontally),
    horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS, Alignment.CenterHorizontally),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    repeat(itemCount) { index ->
      val isActive = index == activePage
      val indicatorWidth by
        animateDpAsState(
          targetValue =
          if (isActive) {
            CarouselConstants.IndicatorActiveWidth
          } else {
            CarouselConstants.IndicatorInactiveWidth
          },
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
