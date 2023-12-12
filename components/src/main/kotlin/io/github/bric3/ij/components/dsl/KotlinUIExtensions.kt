/*
 * IntelliJ Platform Swing Components
 *
 * Copyright (c) 2023 - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package io.github.bric3.ij.components.dsl

import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.impl.ActionButtonWithText
import com.intellij.ui.components.AnActionLink
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.Row
import org.jetbrains.annotations.NonNls

fun Row.actionButtonWithText(action: AnAction, @NonNls actionPlace: String = ActionPlaces.UNKNOWN): Cell<ActionButtonWithText> {
    val component = ActionButtonWithText(action, action.templatePresentation.clone(), actionPlace, ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE)
    return cell(component)
}

// Note, paint a border when focused
// typically when a tab inside a JBTabs is focused, this also paints the border
// of this action link
fun Row.actionLink(action: AnAction, @NonNls actionPlace: String = ActionPlaces.UNKNOWN): Cell<AnActionLink> {
    return cell(AnActionLink(action, actionPlace))
}