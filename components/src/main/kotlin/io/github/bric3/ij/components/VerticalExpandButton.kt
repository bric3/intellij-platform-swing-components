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
package io.github.bric3.ij.components

import com.intellij.icons.AllIcons
import com.intellij.ide.ui.UISettings
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.observable.properties.ObservableMutableProperty
import com.intellij.openapi.observable.util.bindVisible
import com.intellij.openapi.observable.util.not
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.util.NlsActions
import com.intellij.ui.Gray
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.JBColor
import com.intellij.ui.SideBorder
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.JBInsets
import com.intellij.util.ui.UIUtil
import org.intellij.lang.annotations.MagicConstant
import org.jetbrains.annotations.Nls
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Insets
import java.awt.Rectangle
import java.awt.RenderingHints
import java.util.function.Supplier
import javax.swing.Icon
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.swing.plaf.basic.BasicButtonUI

/**
 * Creates a vertical button that can be used to expand (as in toggle) a component.
 *
 * The component is heavily inspired by IntelliJ's
 * [`git4idea.ui.branch.dashboard.ExpandStripeButton`](https://github.com/JetBrains/intellij-community/blob/70a4451b8d46f24b090b04fa8b0611c49f5b23b0/plugins/git4idea/src/git4idea/ui/branch/dashboard/ExpandStripeButton.kt).
 * The UI in particular is from IJ, but this component has an improved API.
 * 
 * @see ExpandableSplitter
 */
class VerticalExpandButton @JvmOverloads constructor(
    @Nls(capitalization = Nls.Capitalization.Title) text: String,
    @SidePlacement val side: Int = SwingConstants.LEFT,
    private val isExpandedProperty: ObservableMutableProperty<Boolean>,
) : JButton(text) {
    /**
     * Whether the component is expanded or not.
     *
     * Read or propagate the value from or to [isExpandedProperty].
     */
    var isExpanded: Boolean
        set(value) = isExpandedProperty.set(value)
        get() = isExpandedProperty.get()
    private val actionIcon: Icon

    init {
        isRolloverEnabled = true
        when (side) {
            SwingConstants.LEFT -> {
                border = IdeBorderFactory.createBorder(JBColor.border(), SideBorder.RIGHT)
                icon = AllIcons.Actions.ArrowExpand
                actionIcon = AllIcons.Actions.ArrowCollapse
            }

            SwingConstants.RIGHT -> {
                border = IdeBorderFactory.createBorder(JBColor.border(), SideBorder.LEFT)
                icon = AllIcons.Actions.ArrowCollapse
                actionIcon = AllIcons.Actions.ArrowExpand
            }

            else -> {
                throw IllegalArgumentException("Invalid side value, use either SwingConstants.LEFT or SwingConstants.RIGHT")
            }
        }

        bindVisible(isExpandedProperty.not())

        addActionListener {
            isExpandedProperty.set(true)
        }
    }

    override fun updateUI() {
        setUI(VerticalButtonUI())

        isOpaque = false
        font = UIUtil.getLabelFont(UIUtil.FontSize.SMALL)
    }

    /**
     * Creates an action that will perform the opposite of the exapnsion
     * i.e. collapse the component.
     */
    fun createCollapseAction(
        dynamicText: Supplier<@NlsActions.ActionText String> = Supplier { "Hide" },
        onCollapseAction: Runnable? = null
    ): AnAction = object : DumbAwareAction(
        dynamicText,
        actionIcon
    ) {
        override fun actionPerformed(e: AnActionEvent) {
            isExpandedProperty.set(false)
            onCollapseAction?.run()
        }
    }

    // Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
    private class VerticalButtonUI : BasicButtonUI() {
        private val iconRect = Rectangle()
        private val textRect = Rectangle()
        private val viewRect = Rectangle()
        private var viewInsets: Insets = JBInsets.create(0, 0)

        override fun getMinimumSize(c: JComponent): Dimension = getPreferredSize(c)
        override fun getMaximumSize(c: JComponent): Dimension = getPreferredSize(c)
        override fun getPreferredSize(c: JComponent): Dimension = ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE

        override fun update(g: Graphics, c: JComponent) {
            val button = c as VerticalExpandButton
            val text = button.text
            val icon = if (button.isEnabled) button.icon else button.disabledIcon

            if (text == null && icon == null) {
                return
            }

            viewInsets = button.getInsets(viewInsets)

            viewRect.x = viewInsets.left
            viewRect.y = viewInsets.top
            viewRect.height = button.width - (viewInsets.left + viewInsets.right)
            viewRect.width = button.height - (viewInsets.top + viewInsets.bottom)

            iconRect.height = 0
            iconRect.width = 0
            iconRect.y = 0
            iconRect.x = 0

            textRect.height = 0
            textRect.width = 0
            textRect.y = 0
            textRect.x = 0

            val fm = button.getFontMetrics(button.font)

            val clippedText = SwingUtilities.layoutCompoundLabel(
                c,
                fm,
                text,
                icon,
                SwingConstants.CENTER,
                if (button.side == SwingConstants.LEFT) SwingConstants.RIGHT else SwingConstants.LEFT,
                SwingConstants.CENTER,
                SwingConstants.TRAILING,
                viewRect,
                iconRect,
                textRect,
                if (text == null) 0 else button.iconTextGap
            )

            val g2 = g.create() as Graphics2D
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)

                iconRect.x = JBUIScale.scale(2 * button.iconTextGap)
                textRect.x -= JBUIScale.scale(2)

                val model = button.model
                g2.color = if (model.isRollover || model.isPressed) HOVER_BACKGROUND_COLOR else button.background
                g2.fillRect(0, 0, button.width, button.height)

                icon?.paintIcon(button, g2, iconRect.y, iconRect.x)

                text?.let {
                    if (button.side == SwingConstants.LEFT) {
                        g2.rotate(-Math.PI / 2)
                        g2.translate(
                            -button.height - iconRect.height - iconRect.x - JBUIScale.scale(button.iconTextGap),
                            0
                        )
                    } else {
                        g2.rotate(Math.PI / 2)
                        g2.translate(iconRect.x, -button.width)
                    }

                    UISettings.setupAntialiasing(g2)
                    g2.color = (if (model.isEnabled) button.foreground else UIManager.getColor("Button.disabledText"))
                    g2.drawString(clippedText, textRect.x, textRect.y + fm.ascent)
                }
            } finally {
                g2.dispose()
            }
        }

        private companion object {
            private val HOVER_BACKGROUND_COLOR = JBColor.namedColor(
                "ToolWindow.Button.hoverBackground",
                JBColor(Gray.x55.withAlpha(40), Gray.x0F.withAlpha(40))
            )
        }
    }

    @MagicConstant(intValues = [LEFT.toLong(), RIGHT.toLong()])
    annotation class SidePlacement
}