// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.grazie

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.ProjectManager
import com.intellij.util.xmlb.annotations.Property
import com.intellij.grazie.ide.msg.GrazieStateLifecycle
import com.intellij.grazie.language.Lang
import com.intellij.grazie.utils.ifTrue

@State(name = "GraziConfig", storages = [Storage("grazi_global.xml")])
class GrazieConfig : PersistentStateComponent<GrazieConfig.State> {
  data class State(@Property val enabledLanguages: Set<Lang> = hashSetOf(Lang.AMERICAN_ENGLISH),
                   @Property val nativeLanguage: Lang = enabledLanguages.first(),
                   @Property val enabledSpellcheck: Boolean = false,
                   @Property val enabledCommitIntegration: Boolean = false,
                   @Property val userWords: Set<String> = HashSet(),
                   @Property val userDisabledRules: Set<String> = HashSet(),
                   @Property val userEnabledRules: Set<String> = HashSet(),
                   @Property val lastSeenVersion: String? = null,
                   val availableLanguages: Set<Lang> = enabledLanguages.filter { it.jLanguage != null }.toSet()) {

    val missedLanguages: Set<Lang>
      get() = enabledLanguages.filter { it.jLanguage == null }.toSet() +
              ((nativeLanguage.jLanguage == null).ifTrue { setOf(nativeLanguage) } ?: emptySet())

    fun clone() = State(
      enabledLanguages = HashSet(enabledLanguages), nativeLanguage = nativeLanguage, enabledSpellcheck = enabledSpellcheck,
      enabledCommitIntegration = enabledCommitIntegration, userWords = HashSet(userWords), userDisabledRules = HashSet(userDisabledRules),
      userEnabledRules = HashSet(userEnabledRules), lastSeenVersion = lastSeenVersion, availableLanguages = availableLanguages)

    fun hasMissedLanguages(withNative: Boolean = true) = (withNative && nativeLanguage.jLanguage == null) || enabledLanguages.any { it.jLanguage == null }

    fun update(enabledLanguages: Set<Lang> = this.enabledLanguages,
               nativeLanguage: Lang = this.nativeLanguage,
               enabledSpellcheck: Boolean = this.enabledSpellcheck,
               enabledCommitIntegration: Boolean = this.enabledCommitIntegration,
               userWords: Set<String> = this.userWords,
               userDisabledRules: Set<String> = this.userDisabledRules,
               userEnabledRules: Set<String> = this.userEnabledRules,
               lastSeenVersion: String? = this.lastSeenVersion) = State(enabledLanguages, nativeLanguage, enabledSpellcheck,
                                                                        enabledCommitIntegration,
                                                                        userWords, userDisabledRules, userEnabledRules, lastSeenVersion,
                                                                        enabledLanguages.filter { it.jLanguage != null }.toSet())
  }

  companion object {
    private val instance: GrazieConfig by lazy { ServiceManager.getService(GrazieConfig::class.java) }

    /**
     * Get copy of Grazie config state
     *
     * Should never be called in GraziStateLifecycle actions
     */
    fun get() = instance.state

    /** Update Grazie config state */
    @Synchronized
    fun update(change: (State) -> State) = instance.loadState(change(get()))
  }

  private var myState = State()

  override fun getState() = myState.clone()

  override fun loadState(state: State) {
    val prevState = myState
    myState = state

    if (prevState != myState) {
      ProjectManager.getInstance().openProjects.forEach { GrazieStateLifecycle.publisher.update(prevState, myState, it) }
    }
  }
}
