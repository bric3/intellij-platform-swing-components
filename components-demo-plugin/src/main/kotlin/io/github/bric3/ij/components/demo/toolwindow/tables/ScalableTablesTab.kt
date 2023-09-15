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

import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.tabs.TabInfo
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.components.BorderLayoutPanel
import io.github.bric3.ij.components.demo.toolwindow.tables.SomeData.englishToJapanese
import io.github.bric3.ij.components.demo.toolwindow.tables.SomeData.japaneseNumbers
import io.github.bric3.ij.components.demo.toolwindow.tables.TableFactory.modelLoadingAsynchronously
import io.github.bric3.ij.components.demo.toolwindow.tables.TableFactory.table
import io.github.bric3.ij.components.table.ScalableJBTable
import io.github.bric3.ij.components.table.ScalableTableView

class ScalableTablesTab : BorderLayoutPanel() {
    init {
        addToCenter(
            panel {
                row {
                    comment("Try zooming, entering/exiting presentation mode, etc.")
                }
                group(ScalableJBTable::class.simpleName!!, false) {
                    row {
                        cell(table(::ScalableJBTable, modelLoadingAsynchronously(englishToJapanese)).second).align(Align.FILL)
                            .comment("This table is a JBTable that scales correctly the custom renderer when to presentation mode")
                    }.resizableRow()
                }.resizableRow()
                group(ScalableTableView::class.simpleName!!, false) {
                    row {
                        cell(table(::ScalableTableView, modelLoadingAsynchronously(japaneseNumbers)).second).align(Align.FILL)
                            .comment("This table is a JBTable that scales correctly the custom renderer when to presentation mode")
                    }.resizableRow()
                }.resizableRow()

                border = JBUI.Borders.empty(5)
            }
        )
    }

    companion object {
        val tabInfo
            get() = TabInfo(ScalableTablesTab()).apply {
                setText("Scalable Tables")
            }
    }
}