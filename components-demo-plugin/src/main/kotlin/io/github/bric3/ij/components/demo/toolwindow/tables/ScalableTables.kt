@file:Suppress("DialogTitleCapitalization")

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

class ScalableTables : BorderLayoutPanel() {
    init {
        addToCenter(
            panel {
                row {
                    comment("Try zooming, entering/exiting presentation mode, etc.")
                }
                group(ScalableJBTable::class.simpleName!!, false) {
                    row {
                        scrollCell(table(::ScalableJBTable, modelLoadingAsynchronously(englishToJapanese))).align(Align.FILL)
                            .comment("This table is a JBTable that scales correctly the custom renderer when to presentation mode")
                    }.resizableRow()
                }.resizableRow()
                group(ScalableTableView::class.simpleName!!, false) {
                    row {
                        scrollCell(table(::ScalableTableView, modelLoadingAsynchronously(japaneseNumbers))).align(Align.FILL)
                            .comment("This table is a JBTable that scales correctly the custom renderer when to presentation mode")
                    }.resizableRow()
                }.resizableRow()

                border = JBUI.Borders.empty(5)
            }
        )
    }

    companion object {
        val tabInfo
            get() = TabInfo(ScalableTables()).apply {
                setText("Scalable Tables")
            }
    }
}