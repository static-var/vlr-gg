package dev.staticvar.designsystem.component.carousel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import dev.staticvar.designsystem.component.card.PrismCard
import dev.staticvar.designsystem.component.card.PrismCardStyle
import dev.staticvar.designsystem.preview.PrismPreview
import dev.staticvar.designsystem.preview.PrismPreviewProvider
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.designsystem.prism.PrismTheme
import dev.staticvar.designsystem.prism.PrismVariant

@PrismPreview
@Composable
internal fun PrismCarouselPreview(@PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant) {
  PrismTheme(variant = variant) {
    Column(
      modifier =
      Modifier.fillMaxSize()
        .background(Prism.color.background)
        .verticalScroll(rememberScrollState())
        .padding(vertical = Prism.dimens.spacingM),
      verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXl),
    ) {
      HeroCarouselPreview()
      MultibrowseCarouselPreview()
      UncontainedCarouselPreview()
    }
  }
}

@Composable
private fun HeroCarouselPreview() {
  CarouselPreviewTitle("Hero Carousel", "Single focal item with hard borders")
  PrismCarousel(
    itemCount = 5,
    variant = PrismCarouselVariant.Hero,
    modifier = Modifier.fillMaxWidth(),
  ) { page ->
    PreviewCarouselCard(
      title = "Featured match ${page + 1}",
      label = "BRUTALIST HERO PANEL",
      style = PrismCardStyle.Outlined,
      modifier = Modifier.fillMaxWidth().height(200.dp).padding(horizontal = Prism.dimens.spacingM),
    )
  }
}

@Composable
private fun MultibrowseCarouselPreview() {
  CarouselPreviewTitle("Multibrowse Carousel", "Center-focused cards with side peeks")
  PrismCarousel(
    itemCount = 4,
    variant = PrismCarouselVariant.Multibrowse,
    modifier = Modifier.fillMaxWidth(),
  ) { page ->
    PreviewCarouselCard(
      title = "Standings block ${page + 1}",
      label = "CENTER EMPHASIS",
      style = PrismCardStyle.Filled,
      modifier = Modifier.fillMaxWidth().height(180.dp).padding(horizontal = Prism.dimens.spacingS),
    )
  }
}

@Composable
private fun UncontainedCarouselPreview() {
  CarouselPreviewTitle("Uncontained Carousel", "Compact rail for quick scan")
  PrismCarousel(
    itemCount = 6,
    variant = PrismCarouselVariant.Uncontained,
    modifier = Modifier.fillMaxWidth(),
    pageSpacing = Prism.dimens.spacingS,
  ) { page ->
    PreviewCarouselCard(
      title = "Item ${page + 1}",
      label = "RAIL",
      style = PrismCardStyle.Outlined,
      modifier = Modifier.fillMaxWidth().height(120.dp).padding(horizontal = Prism.dimens.spacingXs),
    )
  }
}

@Composable
private fun CarouselPreviewTitle(title: String, subtitle: String) {
  Text(
    title,
    style = Prism.typography.sectionTitle,
    color = Prism.color.titleColor,
    modifier = Modifier.padding(horizontal = Prism.dimens.spacingM),
  )
  Text(
    subtitle,
    style = Prism.typography.caption,
    color = Prism.color.labelColor,
    modifier = Modifier.padding(horizontal = Prism.dimens.spacingM),
  )
}

@Composable
private fun PreviewCarouselCard(title: String, label: String, style: PrismCardStyle, modifier: Modifier) {
  PrismCard(modifier = modifier, style = style) {
    Text(title, style = Prism.typography.cardTitle)
    Text(label, style = Prism.typography.caption, color = Prism.color.labelColor)
  }
}
