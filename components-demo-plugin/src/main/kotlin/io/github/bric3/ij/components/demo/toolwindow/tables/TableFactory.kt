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

import com.intellij.openapi.actionSystem.ActionToolbarPosition
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.invokeLater
import com.intellij.ui.JBColor
import com.intellij.ui.TableSpeedSearch
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.table.JBTable
import com.intellij.ui.table.TableView
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.ListTableModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.table.TableCellRenderer
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal object TableFactory {
    private val cs = CoroutineScope(SupervisorJob())

    fun <T> table(tableConstructor: (ListTableModel<T>) -> JBTable, model: ListTableModel<T>): Pair<JTable, JPanel> {
        return tableConstructor(model).run {
            TableSpeedSearch.installOn(this)

            // If not a subtype of TableView (which is a JBTable), then we need to set the renderer
            if (this !is TableView<*>) {
                model.columnInfos.forEachIndexed { index, columnInfo ->
                    // ignoring item, as it's always the same renderer
                    columnModel.getColumn(index).cellRenderer = columnInfo.getRenderer(null)
                }
            }

            val decorated = ToolbarDecorator.createDecorator(this).also {
                it.setToolbarPosition(ActionToolbarPosition.LEFT)
                it.setPanelBorder(JBUI.Borders.customLineTop(JBColor.border()))
                it.setScrollPaneBorder(JBUI.Borders.empty())

                it.disableUpDownActions()

                it.setAddActionName("Add")
                it.setAddAction {
                    // todo
                }

                it.setRemoveActionName("Remove")
                it.setRemoveAction {
                    // todo
                }
                it.setRemoveActionUpdater {
                    this.selectedRowCount > 0
                }
            }.createPanel()
            this to decorated
        }
    }

    fun numberScaleRender(): TableCellRenderer {
        return CompositeTableCellRenderer(
            center = NumberMappingTextRenderer(),
            south = NumberLogScaleRenderer(),
        )
    }

    fun model(
        dataset: List<NumberMapping>,
        tableCellRenderer: TableCellRenderer? = null
    ): ListTableModel<NumberMapping> {
        return ListTableModel(
            arrayOf(NumberMappingColumnInfo("Numbers", tableCellRenderer)),
            dataset
        )
    }

    fun modelLoadingAsynchronously(
        dataset: List<NumberMapping>,
        tableCellRenderer: TableCellRenderer? = null
    ): ListTableModel<NumberMapping> {
        val model = ListTableModel<NumberMapping>(
            NumberMappingColumnInfo("Numbers", tableCellRenderer)
        )

        cs.launch {
            while (true) {
                dataset.forEach {
                    withContext(Dispatchers.EDT) {
                        model.addRow(it)
                    }
                    delay(250.milliseconds)
                }
                delay(60.seconds)
                withContext(Dispatchers.EDT) {
                    invokeLater {
                        model.items = mutableListOf()
                    }
                }
            }
        }
        return model
    }

    class NumberMappingColumnInfo(name: String, tableCellRenderer: TableCellRenderer? = null) : ColumnInfo<NumberMapping, NumberMapping>(name) {
        private val sharedRenderer = tableCellRenderer
        override fun valueOf(item: NumberMapping?): NumberMapping? {
            return item
        }

        override fun getRenderer(item: NumberMapping?): TableCellRenderer? {
            return sharedRenderer
        }

        override fun getComparator(): Comparator<NumberMapping>? {
            // returning null to disable comparison, as it breaks the sorter ¯\_(ツ)_/¯
            // java.lang.NullPointerException: Cannot read field "modelIndex" because "this.viewToModel[index]" is null
            // 	at java.desktop/javax.swing.DefaultRowSorter.convertRowIndexToModel(DefaultRowSorter.java:508)
            // 	at java.desktop/javax.swing.JTable.convertRowIndexToModel(JTable.java:2687)
            // 	at com.intellij.ui.table.TableView.getRow(TableView.java:254)
            // 	at com.intellij.ui.table.TableView.getCellRenderer(TableView.java:75)
            // 	at java.desktop/javax.swing.JTable.getToolTipText(JTable.java:3439)
            // 	at com.intellij.ide.IdeTooltipManager.isTooltipDefined(IdeTooltipManager.java:261)
            // 	at com.intellij.ide.IdeTooltipManager.showForComponent(IdeTooltipManager.java:230)
            // 	at com.intellij.ide.IdeTooltipManager.maybeShowFor(IdeTooltipManager.java:217)
            // 	at com.intellij.ide.IdeTooltipManager.eventDispatched(IdeTooltipManager.java:140)
            // 	at java.desktop/java.awt.Toolkit$SelectiveAWTEventListener.eventDispatched(Toolkit.java:2202)
            // return comparing(NumberMapping::first)
            //     .thenComparing(
            //         comparing(
            //             NumberMapping::second,
            //             java.lang.String.CASE_INSENSITIVE_ORDER
            //         )
            //     ).thenComparing(
            //         comparing(
            //             NumberMapping::third,
            //             java.lang.String.CASE_INSENSITIVE_ORDER
            //         )
            //     )
            return null
        }
    }
}