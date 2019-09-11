// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.grazie.ide.language.javascript

import com.intellij.grazie.GrazieBundle
import com.intellij.grazie.grammar.GrammarChecker
import com.intellij.grazie.ide.language.LanguageSupport
import com.intellij.lang.javascript.psi.JSLiteralExpression
import com.intellij.lang.javascript.psi.ecma6.JSStringTemplateExpression
import com.intellij.psi.PsiElement

class JsStringSupport : LanguageSupport(GrazieBundle.langConfig("global.literal_string.disabled")) {
  override fun isRelevant(element: PsiElement) = element is JSLiteralExpression

  override fun check(element: PsiElement) = when (element) {
    is JSStringTemplateExpression -> {
      val textRanges = element.stringRanges
      GrammarChecker.default.check(
        element,
        tokenRules = GrammarChecker.TokenRules(
          ignoreByIndex = linkedSetOf(
            { _, index ->
              textRanges.all { range -> index !in range }
            }
          )
        )
      )
    }
    else -> GrammarChecker.default.check(element)
  }
}
