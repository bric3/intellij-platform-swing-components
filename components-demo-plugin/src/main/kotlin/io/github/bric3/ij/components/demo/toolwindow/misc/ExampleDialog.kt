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
package io.github.bric3.ij.components.demo.toolwindow.misc

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import io.github.bric3.ij.components.dialog.DialogWrapper2
import javax.swing.Action
import javax.swing.JComponent

class ExampleDialog(project: Project) : DialogWrapper2(project, true) {
    init {
        title = "Example Dialog"
        @Suppress("IncorrectParentDisposable") // otherwise test fails
        Disposer.register(project, disposable)
        init()
    }

    override fun createCenterPanel(): JComponent {
        return panel {
            row {
                icon(AllIcons.General.QuestionDialog).align(AlignX.LEFT)
                text("This is a question")
            }
        }
    }

    /**
     * Force these actions to be on the left side of the dialog,
     * otherwise on Mac the cancel action is forcibly moved on the left.
     */
    override fun createLeftSideActions(): Array<Action> {
        return arrayOf()
    }

    override fun createActions(): Array<Action> {
        return buildList {
            add(swingAction(
                DumbAwareAction.create("View Current") {
                    // no-op
                }
            ))
            add(swingAction(
                DumbAwareAction.create("Delete Current") {
                    // no-op
                }
            ))

            add(swingChoiceAction(
                swingAction(DumbAwareAction.create("Make Read-Only") {
                    // no-op
                }),
                swingAction(DumbAwareAction.create("Make Executable") {
                    // no-op
                }),
                swingAction(DumbAwareAction.create("Make Read-Write") {
                    // no-op
                }),
            ).makeDefault())

            add(cancelAction) // on macOs automatically moved the left only if there's an ok action
        }.toTypedArray()
    }

    override fun postponeValidation() = false
}
