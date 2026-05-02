package dev.staticvar.designsystem.component.table

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import dev.staticvar.designsystem.preview.PrismPreview
import dev.staticvar.designsystem.preview.PrismPreviewProvider
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.designsystem.prism.PrismTheme
import dev.staticvar.designsystem.prism.PrismVariant

@PrismPreview
@Composable
internal fun PrismTablePreview(
  @PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant,
) {
  PrismTheme(variant = variant) {
    val viewState = prismTablePreviewViewState()

    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .background(Prism.color.background)
          .padding(Prism.dimens.spacingM),
      verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
    ) {
      Text(
        text = "Sticky table (first column)",
        style = Prism.typography.label,
        color = Prism.color.labelColor,
      )
      PrismTable(
        modifier =
          Modifier
            .fillMaxWidth()
            .height(280.dp),
        columns = previewColumns,
        rows = previewRows,
        options =
          PrismTableOptions(
            stickyFirstColumn = true,
            rowHeight = 68.dp,
            bodyMaxLines = 2,
          ),
        viewState = viewState,
      )
    }
  }
}

@Composable
private fun prismTablePreviewViewState(): PrismTableViewState {
  val headerStyle = prismTablePreviewHeaderStyle()
  val highlightContentColor = Prism.color.success

  return PrismTableDefaults.viewState(
    palette =
      PrismTablePalette(
        primary =
          PrismTableColorGroup(
            containerColor = Prism.color.surface,
            contentColor = Prism.color.titleColor,
            stickyContainerColor = Prism.color.accentSubtle,
            stickyContentColor = Prism.color.titleColor,
          ),
        secondary =
          PrismTableColorGroup(
            containerColor = Prism.color.backgroundElevated,
            contentColor = Prism.color.bodyColor,
            stickyContainerColor = Prism.color.surfaceVariant,
            stickyContentColor = Prism.color.titleColor,
          ),
      ),
    borderThickness = Prism.dimens.strokeDefault,
    borderColor = Prism.color.strokeVariant,
    cellStyleResolver =
      PrismTableCellStyleResolver { context ->
        prismTablePreviewCellStyle(
          context = context,
          headerStyle = headerStyle,
          highlightContentColor = highlightContentColor,
        )
      },
  )
}

private fun prismTablePreviewCellStyle(
  context: PrismTableCellContext,
  headerStyle: PrismTableCellStyle,
  highlightContentColor: androidx.compose.ui.graphics.Color,
): PrismTableCellStyle? =
  when {
    context.isHeader -> headerStyle
    context.columnIndex == PrismTablePreviewConstants.ScoreColumnIndex ->
      PrismTableCellStyle(contentColor = highlightContentColor)
    else -> null
  }

@Composable
private fun prismTablePreviewHeaderStyle(): PrismTableCellStyle =
  PrismTableCellStyle(
    containerColor = Prism.color.accent,
    contentColor = Prism.color.contentPrimary,
    stickyContainerColor = Prism.color.accentVariant,
    stickyContentColor = Prism.color.contentPrimary,
  )

private val previewColumns =
  listOf(
    PrismTableColumn(
      key = "team",
      title = "Team",
      width = 164.dp,
    ),
    PrismTableColumn(
      key = "map",
      title = "Map",
      width = 120.dp,
      textAlign = TextAlign.Center,
    ),
    PrismTableColumn(
      key = "score",
      title = "Score",
      width = 120.dp,
      textAlign = TextAlign.Center,
    ),
    PrismTableColumn(
      key = "rounds",
      title = "Rounds",
      width = 120.dp,
      textAlign = TextAlign.Center,
    ),
    PrismTableColumn(
      key = "econ",
      title = "Economy",
      width = 132.dp,
      textAlign = TextAlign.Center,
    ),
  )

private val previewRows =
  listOf(
    PrismTableRow("sen-loud", mapOf("team" to "Sentinels\nAmericas", "map" to "Ascent\nOT", "score" to "13-10", "rounds" to "23", "econ" to "82.4\n(+4.1)")),
    PrismTableRow("g2-prx", mapOf("team" to "G2 Esports\nAmericas", "map" to "Bind\nRegulation", "score" to "8-13", "rounds" to "21", "econ" to "77.1\n(-1.8)")),
    PrismTableRow("fnc-th", mapOf("team" to "Fnatic\nEMEA", "map" to "Sunset\nOT", "score" to "14-12", "rounds" to "26", "econ" to "79.3\n(+0.7)")),
    PrismTableRow("edg-gen", mapOf("team" to "EDward Gaming\nChina", "map" to "Lotus\nRegulation", "score" to "11-13", "rounds" to "24", "econ" to "74.8\n(-3.5)")),
    PrismTableRow("drx-kc", mapOf("team" to "DRX\nPacific", "map" to "Haven\nRegulation", "score" to "13-9", "rounds" to "22", "econ" to "80.7\n(+2.2)")),
    PrismTableRow("tl-fut", mapOf("team" to "Team Liquid\nEMEA", "map" to "Icebox\nRegulation", "score" to "13-11", "rounds" to "24", "econ" to "78.6\n(+0.1)")),
    PrismTableRow("lev-c9", mapOf("team" to "Leviatán\nAmericas", "map" to "Split\nRegulation", "score" to "7-13", "rounds" to "20", "econ" to "72.0\n(-4.0)")),
    PrismTableRow("nrg-t1", mapOf("team" to "NRG\nAmericas", "map" to "Pearl\nRegulation", "score" to "13-5", "rounds" to "18", "econ" to "84.2\n(+5.4)")),
    PrismTableRow("koi-bbl", mapOf("team" to "KOI\nEMEA", "map" to "Abyss\nRegulation", "score" to "10-13", "rounds" to "23", "econ" to "75.9\n(-1.1)")),
    PrismTableRow("fpx-vit", mapOf("team" to "FunPlus Phoenix\nChina", "map" to "Breeze\nRegulation", "score" to "13-7", "rounds" to "20", "econ" to "81.1\n(+2.8)")),
  )

private object PrismTablePreviewConstants {
  const val ScoreColumnIndex: Int = 2
}
