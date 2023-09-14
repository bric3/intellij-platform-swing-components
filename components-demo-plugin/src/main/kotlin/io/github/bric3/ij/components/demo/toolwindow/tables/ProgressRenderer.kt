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
import java.awt.BorderLayout
import java.awt.Color
import javax.swing.JComponent
import javax.swing.JTable
import javax.swing.SwingConstants
import javax.swing.event.TableModelEvent
import javax.swing.event.TableModelListener
import javax.swing.table.TableCellRenderer

class ProgressRenderer : BorderLayoutPanel(), TableCellRenderer {
    private val htmlConverter = HtmlToSimpleColoredComponentConverter()

    private val simpleColoredComponent = SimpleColoredComponent().apply {
        border = JBUI.Borders.empty(4)
    }

    private val jProgressBar = ColoredJProgressBar(0, 100)
    private var logScale: LogScale? = null
    private var currentMax = -1L

    @Suppress("UNCHECKED_CAST", "ObjectLiteralToLambda") // obect needed to auto-register in the model
    private val myTableModelListener = object : TableModelListener {
        override fun tableChanged(e: TableModelEvent) {
            computeMaxValue(e.source as ListTableModel<NumberMapping>)
        }
    }

    private fun computeMaxValue(listTableModel: ListTableModel<NumberMapping>) {
        logScale = when (val max = ArrayList(listTableModel.items).maxBy(NumberMapping::first)) {
            null -> null
            else -> LogScale(0, max.first)
        }
    }

    init {
        isOpaque = true
        border = JBUI.Borders.empty(2)
        add(simpleColoredComponent, BorderLayout.CENTER)
        add(jProgressBar, BorderLayout.SOUTH)
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
        (table.model as ListTableModel<NumberMapping>).run {
            if (logScale == null) {
                computeMaxValue(this)
            }
            if (myTableModelListener !in tableModelListeners) {
                addTableModelListener(myTableModelListener)
            }
        }

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

        val logarithmicPercentage = logaritmicPercentageOf(a)
        jProgressBar.value = logarithmicPercentage
        jProgressBar.finishedColor = randomColor(logarithmicPercentage)

        return this
    }

    private fun randomColor(value: Int): Color? {
        return Color.getHSBColor(
            0.1f,
            value.toFloat() / 100,
            0.9f
        )
    }

    fun logaritmicPercentageOf(value: Long): Int {
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
