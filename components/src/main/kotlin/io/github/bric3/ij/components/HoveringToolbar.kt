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
import com.intellij.ui.ColorUtil
import com.intellij.ui.ComponentUtil
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLayeredPane
import com.intellij.ui.hover.TableHoverListener
import com.intellij.util.ui.JBUI
import java.awt.Color
import java.awt.Component
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.MouseWheelEvent
import java.awt.event.MouseWheelListener
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.OverlayLayout
import javax.swing.SwingUtilities
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener

/**
 * Wraps a [JTable] and a [ActionToolbar] to display the toolbar when hovering a specific row.
 *
 * Relies on [TableHoverListener] to discover the hovered table.
 *
 * @see TableHoverListener
 */
@Suppress("UnstableApiUsage")
class HoveringToolbar private constructor(
    val container: JComponent, // TODO replace by getParent
    val table: JTable,
    val toolbar: ActionToolbar
) : JBLayeredPane() {
    private val hoveringToolbarComponent: JComponent = toolbar.component.apply {
        isOpaque = true
        background = ColorUtil.toAlpha(JBColor.MAGENTA, 20)
        border = JBUI.Borders.empty()
    }
    private val hoverLayer: JPanel = JPanel().apply {
        layout = null
        isOpaque = false
        isVisible = false
        background = null
        add(hoveringToolbarComponent)
    }
    private var showToolbar = true

    var isOpaqueToolbarWhenSelected: Boolean = false
    var isToolbarOpaque: Boolean = false
    var toolbarBackground: Color? = null

    init {
        layout = OverlayLayout(this)
        toolbar.targetComponent = table

        table.rowHeight = table.rowHeight.coerceAtLeast(ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE.height + 4)
        ToolbarTableHoverListener().also {
            it.addTo(table)
            ComponentUtil.getScrollPane(table)?.addMouseWheelListener(it)
            table.selectionModel.addListSelectionListener(it)
            table.addPropertyChangeListener("enabled", it)
        }

        isOpaque = false
        // Note kotlin compiles this to the wrong method, as [*_LAYER]'s type is [Integer],
        // but they are constraints not indexes, as such they have to be cast to [Any]
        // to make sure [add(Component, Object)] is called
        add(container, DEFAULT_LAYER as Any)
        add(hoverLayer, PALETTE_LAYER as Any)
    }

    private fun showOrHideToolbar(table: JTable, rowOld: Int, rowNew: Int) {
        if (rowNew == rowOld) return
        if (rowNew != -1) {
            hideToolbar()
        }

        if (!showToolbar) {
            return
        }

        hoverLayer.isVisible = true
        toolbar.updateActionsImmediately() // has to happen after container is set to be visible
        // Adjust toolbar location, and background
        hoveringToolbarComponent.apply {
            isOpaque = isToolbarOpaque
            background = toolbarBackground

            if (isOpaqueToolbarWhenSelected) {
                isOpaque = table.selectedRows.contains(rowNew)
            }
        
            updateBounds(table, rowNew)
            revalidate()
            repaint()
        }
    }

    private fun hideToolbar() {
        hoverLayer.isVisible = false
        // clear previous location
        hoverLayer.repaint(
            hoveringToolbarComponent.x,
            hoveringToolbarComponent.y,
            hoveringToolbarComponent.width,
            hoveringToolbarComponent.height
        )
    }

    private fun JComponent.updateBounds(table: JTable, row: Int) {
        val lastCellRect = when {
            row < 0 -> return
            else -> table.getCellRect(row, table.columnCount - 1, false)
        }
        preferredSize = preferredSize.apply {
            height = table.getRowHeight(row)
        }
        size = preferredSize
        bounds = Rectangle(
            SwingUtilities.convertPoint(
                table,
                table.width - width - 2,
                lastCellRect.y + 1,
                hoverLayer
            ),
            preferredSize
        )
    }

    private inner class ToolbarTableHoverListener
        : TableHoverListener(),
        MouseWheelListener,
        ListSelectionListener,
        PropertyChangeListener {

        private val defaultTableHoverListener = DEFAULT as TableHoverListener

        override fun onHover(table: JTable, rowNew: Int, column: Int) {
            val rowOld = getHoveredRow(table)
            defaultTableHoverListener.onHover(table, rowNew, column)
            showOrHideToolbar(table, rowOld, rowNew)
            return
        }

        override fun mouseWheelMoved(e: MouseWheelEvent) {
            val p = Point(e.xOnScreen, e.yOnScreen).apply {
                SwingUtilities.convertPointFromScreen(this, table)
            }
            mouseMoved(table, p.x, p.y)
        }

        override fun valueChanged(e: ListSelectionEvent) {
            val hoveredRow = getHoveredRow(table)
            showOrHideToolbar(table, -1, hoveredRow)
        }

        override fun propertyChange(it: PropertyChangeEvent) {
            if (it.propertyName != "enabled") return
            showToolbar = it.newValue as Boolean
            val hoveredRow = getHoveredRow(table)
            showOrHideToolbar(table, -1, hoveredRow)
        }
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

    companion object {
        fun wrap(container: JComponent, table: JTable, serviceHoveringToolbar: ActionToolbar): HoveringToolbar {
            return HoveringToolbar(container, table, serviceHoveringToolbar)
        }
    }
}