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

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.ui.HtmlToSimpleColoredComponentConverter
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.ListTableModel
import com.intellij.util.ui.components.BorderLayoutPanel
import io.github.bric3.ij.components.ColoredJProgressBar
import io.github.bric3.ij.components.utils.LogScale
import java.awt.Color
import javax.swing.JComponent
import javax.swing.JTable
import javax.swing.SwingConstants
import javax.swing.event.TableModelEvent
import javax.swing.event.TableModelListener
import javax.swing.table.TableCellRenderer

internal class CompositeTableCellRenderer(
    private val center: TableCellRenderer? = null,
    private val north: TableCellRenderer? = null,
    private val south: TableCellRenderer? = null,
    private val east: TableCellRenderer? = null,
    private val west: TableCellRenderer? = null,
) : BorderLayoutPanel(), TableCellRenderer {

    init {
        isOpaque = true
        border = JBUI.Borders.empty(2)
    }

    override fun getTableCellRendererComponent(
        table: JTable,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int,
    ): JComponent {
        removeAll()
        center?.run { getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column) }?.let {
            addToCenter(it)
        }
        north?.run { getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column) }?.let {
            addToTop(it)
        }
        south?.run { getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column) }?.let {
            addToBottom(it)
        }
        east?.run { getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column) }?.let {
            addToRight(it)
        }
        west?.run { getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column) }?.let {
            addToLeft(it)
        }
        background = if (isSelected)
            JBUI.CurrentTheme.Table.Selection.background(false)
        else
            null
        return this
    }
}

class NumberMappingTextRenderer : TableCellRenderer {
    private val htmlConverter = HtmlToSimpleColoredComponentConverter()

    private val simpleColoredComponent = SimpleColoredComponent().apply {
        border = JBUI.Borders.empty(4)
    }

    @Suppress("UNCHECKED_CAST")
    override fun getTableCellRendererComponent(
        table: JTable,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int,
    ): JComponent? {
        val (a, b, c) = value as? NumberMapping ?: return null
        simpleColoredComponent.apply {
            clear()
            setTextAlign(SwingConstants.LEFT)

            htmlConverter.appendHtml(
                this,
                "$a:&nbsp;",
                SimpleTextAttributes.GRAYED_BOLD_ATTRIBUTES
            )

            append(
                b,
                SimpleTextAttributes.fromTextAttributes(
                    EditorColorsManager.getInstance().globalScheme.getAttributes(
                        DefaultLanguageHighlighterColors.KEYWORD
                    )
                )
            )
            append(" - ")
            val isHiragana = c.isNotEmpty() && Character.UnicodeBlock.of(c.codePointAt(0)) == Character.UnicodeBlock.HIRAGANA
            if (isHiragana) append("「")
            append(
                c,
                SimpleTextAttributes.fromTextAttributes(
                    EditorColorsManager.getInstance().globalScheme.getAttributes(
                        DefaultLanguageHighlighterColors.STRING
                    )
                )
            )
            if (isHiragana) append("」")
        }
        simpleColoredComponent.background = if (isSelected)
            JBUI.CurrentTheme.Table.Selection.background(false)
        else
            null
        return simpleColoredComponent
    }
}

@Suppress("UNCHECKED_CAST", "ObjectLiteralToLambda") // object needed to auto-register in the model
class NumberLogScaleRenderer : TableCellRenderer {
    private val jProgressBar = ColoredJProgressBar(0, 100)
    private var logScale: LogScale? = null

    private val myTableModelListener = object : TableModelListener {
        override fun tableChanged(e: TableModelEvent) {
            computeMaxValue(e.source as ListTableModel<NumberMapping>)
        }
    }

    override fun getTableCellRendererComponent(
        table: JTable,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int
    ): JComponent? {
        (table.model as ListTableModel<NumberMapping>).run {
            if (logScale == null) {
                computeMaxValue(this)
            }
            if (myTableModelListener !in tableModelListeners) {
                addTableModelListener(myTableModelListener)
            }
        }

        val (a, b, c) = value as? NumberMapping ?: return null
        val logarithmicPercentage = logarithmicPercentageOf(a)
        jProgressBar.value = logarithmicPercentage
        jProgressBar.finishedColor = colorIntensity(logarithmicPercentage)

        jProgressBar.background = if (isSelected)
            JBUI.CurrentTheme.Table.Selection.background(false)
        else
            null

        return jProgressBar
    }

    private fun computeMaxValue(listTableModel: ListTableModel<NumberMapping>) {
        logScale = when (val max = ArrayList(listTableModel.items).maxByOrNull(NumberMapping::first)) {
            null -> null
            else -> LogScale(0, max.first)
        }
    }

    private fun colorIntensity(value: Int): Color? {
        return Color.getHSBColor(
            0.1f,
            value.toFloat() / 100,
            0.9f
        )
    }

    private fun logarithmicPercentageOf(value: Long): Int {
        val p = when (val ls = logScale) {
            null -> 0
            else -> ls.linearToLogarithmic(value) * 100
        }
        return p.toInt()
    }

    private fun TextAttributesKey.toColor(): Color? {
        return EditorColorsManager.getInstance()?.globalScheme?.let {
            val attributes = it.getAttributes(this)
            val stripe = attributes?.errorStripeColor
            if (stripe != null) return stripe
            if (attributes != null) {
                val effectColor = attributes.effectColor
                if (effectColor != null) {
                    return effectColor
                }
                val foregroundColor = attributes.foregroundColor
                return foregroundColor ?: attributes.backgroundColor
            }
            return null
        }
    }
}
