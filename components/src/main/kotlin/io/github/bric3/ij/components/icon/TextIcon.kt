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
package io.github.bric3.ij.components.icon

import com.intellij.openapi.util.SystemInfo
import com.intellij.util.ui.JBInsets
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.Color
import java.awt.Component
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.font.FontRenderContext
import java.awt.font.LineMetrics
import java.awt.geom.Rectangle2D
import java.awt.geom.RoundRectangle2D
import javax.swing.Icon
import javax.swing.UIManager

/**
 * TextIcon that uses font metrics to calculate the text baseline and the height of the background.
 *
 * Derived from com.intellij.ui.TextIcon.
 */
class TextIcon(
    val text: String,
    val font: Font,
    private val foregroundColor: Color,
    private val backgroundColor: Color,
    private val arcw: Double = 20.0,
    private val arch: Double = 24.0
) : Icon {

    private var bounds: Rectangle2D = Rectangle2D.Double()

    private val insets: JBInsets = JBUI.insets(3, 8, 2, 8)

    private val reusableShape = RoundRectangle2D.Double()

    override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
        val g2 = g.create() as Graphics2D

        g2.font = font
        val frc = createFontRenderContext()
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, frc.antiAliasingHint)
        val textLcdContrast = UIManager.get(RenderingHints.KEY_TEXT_LCD_CONTRAST)
            ?: UIUtil.getLcdContrastValue() // L&F is not properly updated
        g2.setRenderingHint(RenderingHints.KEY_TEXT_LCD_CONTRAST, textLcdContrast)
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, frc.fractionalMetricsHint)
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        if (bounds.isEmpty) {
            bounds = getTextBounds()
        }
        g2.color = backgroundColor

        reusableShape.setRoundRect(
            /* x = */ x.toDouble(),
            /* y = */ y.toDouble(),
            /* w = */ (bounds.width + insets.left + insets.right),
            /* h = */ (bounds.height + insets.top + insets.bottom),
            /* arcw = */ arcw,
            /* arch = */ arch
        )
        g2.fill(reusableShape)
        g2.color = foregroundColor


        val textX = (x + insets.left).toFloat()

        val fm = font.getLineMetrics(text, frc)
        val textY = y + insets.top + getTextBaseLine(fm, bounds.height.toInt())

        g2.drawString(text, textX, textY)

        g2.dispose()
    }

    // Copy from com.intellij.ui.SimpleColoredComponent.getTextBaseLine, but uses LineMetrics, and returns a float
    private fun getTextBaseLine(metrics: LineMetrics, height: Int): Float {
        // adding leading to ascent, just like in editor (leads to bad presentation for certain fonts with Oracle JDK, see IDEA-167541)
        return (height - metrics.height + 1) / 2 + metrics.ascent +
                (if (SystemInfo.isJetBrainsJvm) metrics.leading else 0f)
    }

    override fun getIconWidth(): Int {
        val bounds = getTextBounds()
        return (insets.left + bounds.width + 1 + insets.right).toInt()
    }

    override fun getIconHeight(): Int {
        val bounds = getTextBounds()
        return (insets.top + bounds.height + 1 + insets.bottom).toInt()
    }

    private fun getTextBounds(): Rectangle2D {
        return font.getStringBounds(text, createFontRenderContext())
    }

    private fun createFontRenderContext(): FontRenderContext {
        val aaHint = UIManager.get(RenderingHints.KEY_TEXT_ANTIALIASING) ?: RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT
        val fmHint =
            UIManager.get(RenderingHints.KEY_FRACTIONALMETRICS) ?: RenderingHints.VALUE_FRACTIONALMETRICS_DEFAULT
        return FontRenderContext(null, aaHint, fmHint)
    }
}