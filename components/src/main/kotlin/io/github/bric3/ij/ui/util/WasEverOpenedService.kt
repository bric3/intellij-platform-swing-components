/*
 * IntelliJ Platform Swing Components
 *
 * Copyright (c) 2024 - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package io.github.bric3.ij.ui.util

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * This is an example of a service that stores a boolean state in the IDE and exposes it as a flow.
 */
@Service(Service.Level.APP)
@State(name = "TabWasOpened", storages = [Storage("was-ever-opened.xml")])
class WasEverOpenedService : PersistentStateComponent<WasEverOpenedService.State> {
    private val _wasEverOpenedStateFlow = MutableStateFlow(false)
    val wasEverOpenedStateFlow = _wasEverOpenedStateFlow.asStateFlow()

    fun setWasOpened(wasOpened: Boolean) {
        this._wasEverOpenedStateFlow.value = wasOpened
    }
    
    override fun getState(): State {
        return State().apply {
            wasEverOpened = _wasEverOpenedStateFlow.value
        }
    }

    override fun loadState(state: State) {
        _wasEverOpenedStateFlow.value = state.wasEverOpened
    }

    class State : BaseState() {
        var wasEverOpened: Boolean by property(false)

        // other type of property delegates
        // @get:Attribute
        // var enumProperty by enum(ThreeState.UNSURE)
        //
        // @get:XMap
        // val mapProperty by linkedMap<String, Boolean>()
    }

    enum class ThreeState {
        YES, NO, UNSURE
    }

    companion object {
        @JvmStatic
        fun getInstance(): WasEverOpenedService = ApplicationManager.getApplication().getService(WasEverOpenedService::class.java)
    }
}