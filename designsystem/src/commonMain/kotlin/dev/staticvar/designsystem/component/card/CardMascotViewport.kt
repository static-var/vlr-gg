/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.card

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutBoundsHolder
import androidx.compose.ui.layout.layoutBounds
import androidx.compose.ui.layout.onVisibilityChangedNode
import androidx.compose.ui.modifier.ModifierLocalModifierNode
import androidx.compose.ui.modifier.modifierLocalOf
import androidx.compose.ui.modifier.modifierLocalProvider
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo

private val LocalCardMascotViewport = modifierLocalOf<LayoutBoundsHolder?> { null }

/** Marks the existing scroll viewport without changing its layout. */
@Composable
public fun Modifier.cardMascotViewport(): Modifier {
  val holder = remember { LayoutBoundsHolder() }
  return modifierLocalProvider(LocalCardMascotViewport) { holder }.layoutBounds(holder)
}

/** Reports whether the mascot footprint fits entirely inside its nearest declared viewport. */
public fun Modifier.onCardMascotVisibilityChanged(onVisibilityChanged: (Boolean) -> Unit): Modifier =
  this then CardMascotVisibilityElement(onVisibilityChanged)

private data class CardMascotVisibilityElement(val onVisibilityChanged: (Boolean) -> Unit) :
  ModifierNodeElement<CardMascotVisibilityNode>() {
  override fun create(): CardMascotVisibilityNode = CardMascotVisibilityNode(onVisibilityChanged)

  override fun update(node: CardMascotVisibilityNode) {
    node.onVisibilityChanged = onVisibilityChanged
    if (node.isAttached) node.updateViewport()
  }

  override fun InspectorInfo.inspectableProperties() {
    name = "onCardMascotVisibilityChanged"
    properties["onVisibilityChanged"] = onVisibilityChanged
  }
}

private class CardMascotVisibilityNode(var onVisibilityChanged: (Boolean) -> Unit) :
  DelegatingNode(), ModifierLocalModifierNode {
  private var viewport: LayoutBoundsHolder? = null
  private var visibilityNode: DelegatableNode? = null

  override fun onAttach() {
    updateViewport()
  }

  fun updateViewport() {
    val currentViewport = LocalCardMascotViewport.current
    if (visibilityNode != null && viewport === currentViewport) return

    visibilityNode?.let { undelegate(it) }
    viewport = currentViewport
    visibilityNode = delegate(
      onVisibilityChangedNode(
        minDurationMs = 0,
        minFractionVisible = 1f,
        viewportBounds = currentViewport,
        callback = { onVisibilityChanged(it) },
      ),
    )
  }

  override fun onDetach() {
    visibilityNode?.let { undelegate(it) }
    visibilityNode = null
    viewport = null
  }
}
