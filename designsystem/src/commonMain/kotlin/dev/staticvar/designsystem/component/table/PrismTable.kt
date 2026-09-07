/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.table

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.staticvar.designsystem.component.frame.prismFrame
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.designsystem.prism.frame.PrismFrameTokens

@Immutable
public data class PrismTableColumn(
  val key: String,
  val title: String,
  val width: Dp = PrismTableDefaults.ColumnWidth,
  val textAlign: TextAlign = TextAlign.Start,
)

@Immutable
public data class PrismTableRow(
  val key: String,
  val cells: Map<String, String>,
  val cellTextAlignments: Map<String, TextAlign> = emptyMap(),
  val onClick: (() -> Unit)? = null,
)

@Immutable
public data class PrismTableOptions(
  val stickyFirstColumn: Boolean = true,
  val rowHeight: Dp = PrismTableDefaults.RowHeight,
  val headerHeight: Dp = PrismTableDefaults.HeaderHeight,
  val bodyMaxLines: Int = PrismTableDefaults.BodyMaxLines,
  val bodyOverflow: TextOverflow = TextOverflow.Ellipsis,
  val headerOverflow: TextOverflow = TextOverflow.Ellipsis,
  val cellPadding: PaddingValues = PrismTableDefaults.cellPadding(),
)

public enum class PrismTableColorRole {
  Primary,
  Secondary,
}

@Immutable
public data class PrismTableColorGroup(
  val containerColor: Color,
  val contentColor: Color,
  val stickyContainerColor: Color = containerColor,
  val stickyContentColor: Color = contentColor,
)

@Immutable
public data class PrismTablePalette(val primary: PrismTableColorGroup, val secondary: PrismTableColorGroup)

@Immutable
public data class PrismTableCellContext(
  val rowIndex: Int,
  val columnIndex: Int,
  val isHeader: Boolean,
  val isStickyColumn: Boolean,
  val columnKey: String = "",
)

@Immutable
public data class PrismTableCellStyle(
  val colorRole: PrismTableColorRole? = null,
  val containerColor: Color? = null,
  val contentColor: Color? = null,
  val stickyContainerColor: Color? = null,
  val stickyContentColor: Color? = null,
)

public fun interface PrismTableCellStyleResolver {
  public fun resolve(context: PrismTableCellContext): PrismTableCellStyle?
}

public fun interface PrismTableCellContentResolver {
  public fun resolve(context: PrismTableCellContext, value: String): (@Composable () -> Unit)?
}

@Immutable
public data class PrismTableViewState(
  val palette: PrismTablePalette,
  val borderThickness: Dp,
  val borderColor: Color,
  val cellStyleResolver: PrismTableCellStyleResolver? = null,
  val frame: PrismFrameTokens = PrismFrameTokens(),
)

public object PrismTableDefaults {
  public val ColumnWidth: Dp = 132.dp
  public val RowHeight: Dp = 48.dp
  public val HeaderHeight: Dp = 52.dp
  public const val BodyMaxLines: Int = 1
  public val CellContentAlignment: Alignment = Alignment.CenterStart
  public val CellPaddingHorizontal: Dp = 12.dp
  public val CellPaddingVertical: Dp = 8.dp

  public fun cellPadding(): PaddingValues =
    PaddingValues(horizontal = CellPaddingHorizontal, vertical = CellPaddingVertical)

  @Composable
  public fun palette(): PrismTablePalette = PrismTablePalette(
    primary =
    PrismTableColorGroup(
      containerColor = Prism.color.surface,
      contentColor = Prism.color.titleColor,
      stickyContainerColor = Prism.color.surfaceVariant,
      stickyContentColor = Prism.color.titleColor,
    ),
    secondary =
    PrismTableColorGroup(
      containerColor = Prism.color.backgroundElevated,
      contentColor = Prism.color.bodyColor,
      stickyContainerColor = Prism.color.surfaceDim,
      stickyContentColor = Prism.color.bodyColor,
    ),
  )

  @Composable
  public fun viewState(
    palette: PrismTablePalette = palette(),
    borderThickness: Dp = Prism.dimens.strokeDefault,
    borderColor: Color = Prism.color.stroke,
    cellStyleResolver: PrismTableCellStyleResolver? = null,
    frame: PrismFrameTokens = Prism.frames.panel,
  ): PrismTableViewState = PrismTableViewState(
    palette = palette,
    borderThickness = borderThickness,
    borderColor = borderColor,
    cellStyleResolver = cellStyleResolver,
    frame = frame,
  )
}

@Composable
public fun PrismTable(
  columns: List<PrismTableColumn>,
  rows: List<PrismTableRow>,
  modifier: Modifier = Modifier,
  options: PrismTableOptions = PrismTableOptions(),
  viewState: PrismTableViewState = PrismTableDefaults.viewState(),
  cellContentResolver: PrismTableCellContentResolver? = null,
) {
  if (columns.isEmpty()) return

  val horizontalScroll = rememberScrollState()
  val verticalScroll = rememberScrollState()
  val firstColumn = columns.first()
  val border = viewState.frame.border ?: BorderStroke(width = viewState.borderThickness, color = viewState.borderColor)
  val indicatorColor = Prism.color.accent
  val indicatorTrackColor = Prism.color.stroke

  Box(
    modifier =
    modifier
      .prismFrame(viewState.frame, Prism.shapes.medium)
      .border(border, Prism.shapes.medium)
      .clip(Prism.shapes.medium)
      .tableScrollIndicator(
        state = horizontalScroll,
        pinnedWidth = if (options.stickyFirstColumn) firstColumn.width else 0.dp,
        color = indicatorColor,
        trackColor = indicatorTrackColor,
        thickness = viewState.borderThickness,
      ),
  ) {
    Column(
      modifier =
      Modifier
        .verticalScroll(verticalScroll)
        .horizontalScroll(horizontalScroll),
    ) {
      HeaderRow(
        columns = columns,
        options = options,
        viewState = viewState,
        cellContentResolver = cellContentResolver,
      )

      rows.forEachIndexed { rowIndex, row ->
        BodyRow(
          row = row,
          rowIndex = rowIndex,
          columns = columns,
          options = options,
          viewState = viewState,
          cellContentResolver = cellContentResolver,
        )
      }
    }

    if (options.stickyFirstColumn) {
      StickyFirstColumn(
        column = firstColumn,
        rows = rows,
        verticalScroll = verticalScroll,
        options = options,
        viewState = viewState,
        cellContentResolver = cellContentResolver,
      )
    }
  }
}

private fun Modifier.tableScrollIndicator(
  state: ScrollState,
  pinnedWidth: Dp,
  color: Color,
  trackColor: Color,
  thickness: Dp,
): Modifier = drawWithContent {
  drawContent()
  if (state.maxValue > 0 && state.maxValue != Int.MAX_VALUE) {
    val inset = thickness.toPx() * 2
    val start = pinnedWidth.toPx() + inset
    val trackWidth = (size.width - start - inset).coerceAtLeast(0f)
    val viewport = (size.width - pinnedWidth.toPx()).coerceAtLeast(0f)
    val thumbWidth = trackWidth * viewport / (viewport + state.maxValue)
    val thumbOffset = (trackWidth - thumbWidth) * state.value / state.maxValue
    val y = size.height - inset - thickness.toPx()
    drawRect(trackColor, Offset(start, y), Size(trackWidth, thickness.toPx()))
    drawRect(color, Offset(start + thumbOffset, y), Size(thumbWidth, thickness.toPx()))
  }
}

@Composable
private fun StickyFirstColumn(
  column: PrismTableColumn,
  rows: List<PrismTableRow>,
  verticalScroll: ScrollState,
  options: PrismTableOptions,
  viewState: PrismTableViewState,
  cellContentResolver: PrismTableCellContentResolver?,
) {
  Column(
    modifier =
    Modifier
      .width(column.width)
      .wrapContentHeight(align = Alignment.Top, unbounded = true)
      .offset { IntOffset(x = 0, y = -verticalScroll.value) }
      .zIndex(PrismTableLayoutConstants.StickyColumnZIndex),
  ) {
    StickyHeaderCell(
      column = column,
      options = options,
      viewState = viewState,
      cellContentResolver = cellContentResolver,
    )
    rows.forEachIndexed { rowIndex, row ->
      StickyBodyCell(
        row = row,
        rowIndex = rowIndex,
        column = column,
        options = options,
        viewState = viewState,
        cellContentResolver = cellContentResolver,
      )
    }
  }
}

@Composable
private fun StickyHeaderCell(
  column: PrismTableColumn,
  options: PrismTableOptions,
  viewState: PrismTableViewState,
  cellContentResolver: PrismTableCellContentResolver?,
) {
  TableCell(
    value = column.title,
    textAlign = column.textAlign,
    modifier = Modifier.fillMaxWidth().height(options.headerHeight),
    options = options,
    viewState = viewState,
    context =
    PrismTableCellContext(
      rowIndex = PrismTableLayoutConstants.HeaderRowIndex,
      columnIndex = PrismTableLayoutConstants.FirstColumnIndex,
      isHeader = true,
      isStickyColumn = true,
      columnKey = column.key,
    ),
    cellContentResolver = cellContentResolver,
  )
}

@Composable
private fun StickyBodyCell(
  row: PrismTableRow,
  rowIndex: Int,
  column: PrismTableColumn,
  options: PrismTableOptions,
  viewState: PrismTableViewState,
  cellContentResolver: PrismTableCellContentResolver?,
) {
  TableCell(
    value = row.cells[column.key].orEmpty(),
    textAlign = row.resolveTextAlign(column),
    modifier = Modifier.fillMaxWidth().height(options.rowHeight),
    options = options,
    viewState = viewState,
    onClick = row.onClick,
    context =
    PrismTableCellContext(
      rowIndex = rowIndex,
      columnIndex = PrismTableLayoutConstants.FirstColumnIndex,
      isHeader = false,
      isStickyColumn = true,
      columnKey = column.key,
    ),
    cellContentResolver = cellContentResolver,
  )
}

@Composable
private fun HeaderRow(
  columns: List<PrismTableColumn>,
  options: PrismTableOptions,
  viewState: PrismTableViewState,
  cellContentResolver: PrismTableCellContentResolver?,
  modifier: Modifier = Modifier,
) {
  Row(modifier = modifier.height(options.headerHeight).requiredWidth(columns.totalWidth())) {
    columns.forEachIndexed { columnIndex, column ->
      TableCell(
        value = column.title,
        textAlign = column.textAlign,
        modifier =
        Modifier
          .width(column.width)
          .fillMaxHeight(),
        options = options,
        viewState = viewState,
        context =
        PrismTableCellContext(
          rowIndex = PrismTableLayoutConstants.HeaderRowIndex,
          columnIndex = columnIndex,
          isHeader = true,
          isStickyColumn = false,
          columnKey = column.key,
        ),
        cellContentResolver = cellContentResolver,
      )
    }
  }
}

@Composable
private fun BodyRow(
  row: PrismTableRow,
  rowIndex: Int,
  columns: List<PrismTableColumn>,
  options: PrismTableOptions,
  viewState: PrismTableViewState,
  cellContentResolver: PrismTableCellContentResolver?,
) {
  Row(modifier = Modifier.height(options.rowHeight)) {
    columns.forEachIndexed { columnIndex, column ->
      TableCell(
        value = row.cells[column.key].orEmpty(),
        textAlign = row.resolveTextAlign(column),
        modifier =
        Modifier
          .width(column.width)
          .fillMaxHeight(),
        options = options,
        viewState = viewState,
        onClick = row.onClick,
        context =
        PrismTableCellContext(
          rowIndex = rowIndex,
          columnIndex = columnIndex,
          isHeader = false,
          isStickyColumn = false,
          columnKey = column.key,
        ),
        cellContentResolver = cellContentResolver,
      )
    }
  }
}

@Composable
private fun TableCell(
  value: String,
  textAlign: TextAlign,
  modifier: Modifier,
  options: PrismTableOptions,
  viewState: PrismTableViewState,
  context: PrismTableCellContext,
  onClick: (() -> Unit)? = null,
  cellContentResolver: PrismTableCellContentResolver? = null,
) {
  val colors = resolveColors(viewState = viewState, context = context)
  val clickModifier = if (!context.isHeader && onClick != null) {
    Modifier.clickable(role = Role.Button, onClick = onClick)
  } else {
    Modifier
  }

  Box(
    modifier =
    modifier
      .then(clickModifier)
      .background(colors.containerColor)
      .border(BorderStroke(width = viewState.borderThickness, color = viewState.borderColor))
      .padding(options.cellPadding),
    contentAlignment = PrismTableDefaults.CellContentAlignment,
  ) {
    cellContentResolver?.resolve(context = context, value = value)?.invoke()
      ?: Text(
        text = value,
        color = colors.contentColor,
        style = if (context.isHeader) Prism.typography.label else Prism.typography.bodySmall,
        maxLines = if (context.isHeader) 1 else options.bodyMaxLines.coerceAtLeast(1),
        overflow = if (context.isHeader) options.headerOverflow else options.bodyOverflow,
        textAlign = textAlign,
        modifier = Modifier.fillMaxWidth(),
      )
  }
}

private fun PrismTableRow.resolveTextAlign(column: PrismTableColumn): TextAlign =
  cellTextAlignments[column.key] ?: column.textAlign

private fun List<PrismTableColumn>.totalWidth(): Dp =
  this.fold(initial = 0.dp) { total, column -> total + column.width }

private fun resolveColors(viewState: PrismTableViewState, context: PrismTableCellContext): TableCellColors {
  val defaultRole =
    if (context.isHeader || context.rowIndex % 2 == PrismTableLayoutConstants.EvenRowModulo) {
      PrismTableColorRole.Primary
    } else {
      PrismTableColorRole.Secondary
    }

  val style = viewState.cellStyleResolver?.resolve(context)
  val colorRole = style?.colorRole ?: defaultRole
  val group =
    when (colorRole) {
      PrismTableColorRole.Primary -> viewState.palette.primary
      PrismTableColorRole.Secondary -> viewState.palette.secondary
    }

  val containerColor =
    if (context.isStickyColumn) {
      style?.stickyContainerColor ?: style?.containerColor ?: group.stickyContainerColor
    } else {
      style?.containerColor ?: group.containerColor
    }
  val contentColor =
    if (context.isStickyColumn) {
      style?.stickyContentColor ?: style?.contentColor ?: group.stickyContentColor
    } else {
      style?.contentColor ?: group.contentColor
    }

  return TableCellColors(containerColor = containerColor, contentColor = contentColor)
}

@Immutable
private data class TableCellColors(val containerColor: Color, val contentColor: Color)

private object PrismTableLayoutConstants {
  const val HeaderRowIndex: Int = -1
  const val FirstColumnIndex: Int = 0
  const val EvenRowModulo: Int = 2
  const val StickyColumnZIndex: Float = 3f
}
