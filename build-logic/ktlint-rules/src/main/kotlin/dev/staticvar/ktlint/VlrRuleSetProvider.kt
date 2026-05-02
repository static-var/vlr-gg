/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.ktlint

import com.pinterest.ktlint.cli.ruleset.core.api.RuleSetProviderV3
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import com.pinterest.ktlint.rule.engine.core.api.RuleSetId

public class VlrRuleSetProvider : RuleSetProviderV3(RuleSetId("vlr")) {
  override fun getRuleProviders(): Set<RuleProvider> = setOf(
    RuleProvider { OneAnnotationPerLineRule() },
  )
}
