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

import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.ui.ComponentUtil
import com.intellij.ui.Gray
import com.intellij.ui.components.JBLayeredPane
import com.intellij.ui.hover.TableHoverListener
import com.intellij.util.ui.JBUI
import java.awt.Component
import java.awt.Rectangle
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.OverlayLayout
import javax.swing.SwingUtilities
import javax.swing.event.MouseInputAdapter

@Suppress("UnstableApiUsage")
class HoveringToolbar private constructor(
    private val container: JComponent, // TODO replace by getParent
    private val table: JTable,
    private val serviceHoveringToolbar: ActionToolbar
) : JBLayeredPane() {
    private val hoveringToolbarComponent: JComponent = serviceHoveringToolbar.component.apply {
        isOpaque = false
        background = JBUI.CurrentTheme.Table.BACKGROUND
        border = JBUI.Borders.empty()
    }
    private val hoveringPanel: JPanel = JPanel().apply {
        layout = null
        isOpaque = false
        isVisible = false
        background = Gray.TRANSPARENT
        add(hoveringToolbarComponent)
    }

    init {
        layout = OverlayLayout(this)

        table.rowHeight = ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE.height + 4
        TableHoverListener.DEFAULT.addTo(table)

        val mouseListener = HoveringToolbarMouseListener().also {
            table.addMouseListener(it)
            table.addMouseMotionListener(it)
        }
        ComponentUtil.getScrollPane(table)?.addMouseWheelListener(mouseListener)
        isOpaque = false
        // Note kotlin compiles this to the wrong method, as [*_LAYER]'s type is [Integer],
        // but they are constraints not indexes, as such they have to be cast to Any
        // to make sure [add(Component, Object)] is called
        add(container, DEFAULT_LAYER as Any)
        add(hoveringPanel, PALETTE_LAYER as Any)
    }

    /**
     * Specific get components to handle a regression in the [TableHoverListener]
     * infrastructure code ([HoverService][com.intellij.ide.HoverService]).
     *
     *
     *
     * In particular since IntelliJ 2022.1 ([HoverService][com.intellij.ide.HoverService])
     * does not walk every component hierarchy, as it assumes component are not used as
     * *overlays*.
     * This has been reported as [IDEA-295116](https://youtrack.jetbrains.com/issue/IDEA-295116/TableHoverListener-is-flickering-when-a-JLayeredPane-is-involved).
     *
     *
     *
     *
     * In order to workaround this issue, the order of components is tweaked to ensure
     * the descendant with the table having the [TableHoverListener] is the first
     * in the returned array.
     *
     *
     * @see [Container#getComponents]
     */
    override fun getComponents(): Array<Component> {
        val components = super.getComponents()
        components.sortWith { c1: Component, _: Component? -> if (c1 === container) -1 else 1 } // TODO replace container by getParent that is not the scroll pane/viewport
        return components
    }

    private inner class HoveringToolbarMouseListener : MouseInputAdapter() {
        override fun mouseWheelMoved(e: MouseWheelEvent) {
            updateHover()
        }

        override fun mouseEntered(e: MouseEvent) {
            updateHover()
        }

        override fun mouseExited(e: MouseEvent) {
            updateHover()
        }

        override fun mouseMoved(e: MouseEvent) {
            updateHover()
        }

        override fun mouseClicked(e: MouseEvent) {
            updateHover()
        }

        override fun mousePressed(e: MouseEvent) {
            updateHover()
        }

        private fun updateHover() {
            val hoveredRow = TableHoverListener.getHoveredRow(table)
            if (hoveredRow == -1) {
                hoveringPanel.isVisible = false
                return
            }
            val firstCellRect = (if (hoveredRow < 0) null else table.getCellRect(hoveredRow, 0, false)) ?: return
            hoveringToolbarComponent.apply {
                isOpaque = table.selectedRows.contains(hoveredRow)
                preferredSize = preferredSize.apply {
                    height = table.getRowHeight(hoveredRow) - 3
                }
                size = preferredSize
                bounds = Rectangle(
                    SwingUtilities.convertPoint(
                        table,
                        table.width - width - 2,
                        firstCellRect.y + 1,
                        hoveringPanel
                    ),
                    preferredSize
                )
            }.apply {
                revalidate()
                repaint()
            }
            hoveringPanel.isVisible = true
            serviceHoveringToolbar.updateActionsImmediately() // has to happen after container is set to be visible
        }
    }

    // private fun forceActionRefresh(me: MouseEvent) {
    //     val context = ActionToolbar.getDataContextFor(serviceHoveringToolbar.targetComponent)
    //     val event = AnActionEvent.createFromInputEvent(
    //         me,
    //         "hovering-toolbar",
    //         serviceHoveringToolbar.actionGroup.templatePresentation,
    //         context,
    //         false,
    //         true
    //     )
    //     if (ActionUtil.lastUpdateAndCheckDumb(action, event, false)) {
    //         ActionUtil.performActionDumbAwareWithCallbacks(action, event)
    //         ActionToolbar.findToolbarBy(this)?.updateActionsImmediately()
    //     }
    // }

    companion object {
        fun wrap(container: JComponent, table: JTable, serviceHoveringToolbar: ActionToolbar): HoveringToolbar {
            return HoveringToolbar(container, table, serviceHoveringToolbar)
        }
    }
}