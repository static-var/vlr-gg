/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.ktlint

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleAutocorrectApproveHandler
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.replaceTextWith
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

public class OneAnnotationPerLineRule :
  Rule(
    ruleId = RuleId("vlr:one-annotation-per-line"),
    about = About(),
  ),
  RuleAutocorrectApproveHandler,
  Rule.OnlyWhenEnabledInEditorconfig {
  override fun beforeVisitChildNodes(
    node: ASTNode,
    emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
  ) {
    when (node.elementType) {
      ElementType.ANNOTATION_ENTRY -> node.wrapAfterPreviousAnnotation(emit)
      ElementType.PROPERTY_ACCESSOR -> node.wrapAccessorAfterAnnotations(emit)
    }
  }

  override fun afterVisitChildNodes(
    node: ASTNode,
    emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
  ): Unit = Unit

  private fun ASTNode.wrapAfterPreviousAnnotation(
    emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
  ) {
    val whitespace = treePrev ?: return
    val previousAnnotation = whitespace.treePrev ?: return
    if (whitespace.elementType != ElementType.WHITE_SPACE) return
    if (previousAnnotation.elementType != ElementType.ANNOTATION_ENTRY) return
    if (whitespace.textContains('\n')) return

    val autocorrectDecision = emit(startOffset, "Annotations must be placed on separate lines", true)
    if (autocorrectDecision == AutocorrectDecision.ALLOW_AUTOCORRECT) {
      whitespace.replaceTextWith("\n${indentAtStart()}")
    }
  }

  private fun ASTNode.wrapAccessorAfterAnnotations(
    emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
  ) {
    val modifierList = findChildByType(ElementType.MODIFIER_LIST) ?: return
    val whitespace = modifierList.treeNext ?: return
    if (whitespace.elementType != ElementType.WHITE_SPACE) return
    if (whitespace.textContains('\n')) return

    val autocorrectDecision = emit(whitespace.startOffset, "Accessor must be placed below its annotations", true)
    if (autocorrectDecision == AutocorrectDecision.ALLOW_AUTOCORRECT) {
      whitespace.replaceTextWith("\n${indentAtStart()}")
    }
  }

  private fun ASTNode.indentAtStart(): String {
    val rootText = rootNode().text
    val lineStart = rootText.lastIndexOf('\n', startOffset - 1).let { index ->
      if (index == -1) 0 else index + 1
    }
    val lineTextBeforeNode = rootText.substring(lineStart, startOffset)
    return lineTextBeforeNode.takeWhile(Char::isWhitespace)
  }

  private fun ASTNode.rootNode(): ASTNode {
    var current = this
    while (current.treeParent != null) {
      current = current.treeParent
    }
    return current
  }
}
