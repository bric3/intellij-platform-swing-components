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
package io.github.bric3.ij.components.demo.toolwindow.tables

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.DumbAwareToggleAction
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.tabs.TabInfo
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.components.BorderLayoutPanel
import io.github.bric3.ij.components.HoveringToolbar
import io.github.bric3.ij.components.table.ScalableTableView
import javax.swing.JComponent

class TableWithHoverToolbarTab : BorderLayoutPanel() {
    init {
        addToCenter(
            panel {
                row {
                    comment("Try hovering the rows")
                }
                val tableWithHoveredToolbar = makeTableWithHoverToolbar(makeServiceHoverToolbar())
                group(HoveringToolbar::class.simpleName!!, false) {
                    row {
                        cell(tableWithHoveredToolbar).align(Align.FILL)
                    }.resizableRow()
                }.resizableRow()

                row {
                    actionButton(object : DumbAwareToggleAction("Enabled") {
                        override fun isSelected(e: AnActionEvent) =
                            tableWithHoveredToolbar.table.isEnabled

                        override fun setSelected(e: AnActionEvent, state: Boolean) {
                            tableWithHoveredToolbar.table.isEnabled = state
                        }

                        override fun getActionUpdateThread() = ActionUpdateThread.EDT
                    })
                }

                border = JBUI.Borders.empty(5)
            }
        )
    }

    private fun makeTableWithHoverToolbar(hoverToolbar: ActionToolbar): HoveringToolbar {
        val (table, decorator) = TableFactory.table(
            ::ScalableTableView,
            TableFactory.model(
                SomeData.japaneseNumbers,
                NumberMappingTextRenderer()
            )
        )

        return HoveringToolbar.wrap(
            decorator,
            table,
            hoverToolbar
        )
    }

    private fun makeServiceHoverToolbar(): ActionToolbar {
        val actionManager = ActionManager.getInstance()
        val moreActionGroup = DefaultActionGroup.createPopupGroupWithEmptyText().also {
            it.templatePresentation.icon = AllIcons.Actions.More
            it.addAll(
                DumbAwareAction.create("Preview", AllIcons.Actions.Preview) { },
                DumbAwareAction.create("Checkout", AllIcons.Actions.CheckOut) { },
                DumbAwareAction.create("Help", AllIcons.Actions.Help) { },
            )
        }
        val actionGroup = DefaultActionGroup(
            DumbAwareAction.create("Start", AllIcons.Actions.Play_forward) { },
            DumbAwareAction.create("Pause", AllIcons.Actions.Pause) { },
            moreActionGroup
        )
        return actionManager.createActionToolbar("hovering-toolbar", actionGroup, true).also {
            it.targetComponent = this
        }
    }


    companion object {
        val tabInfo
            get() = TabInfo(TableWithHoverToolbarTab()).apply {
                setText("Table with Hover Toolbar")
            }
    }
}
