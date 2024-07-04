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
package io.github.bric3.ij.components.combo

import com.intellij.openapi.ui.popup.util.PopupUtil
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.ListSpeedSearch
import com.intellij.ui.ScrollingUtil
import com.intellij.ui.components.JBList
import com.intellij.ui.hover.ListHoverListener
import com.intellij.ui.render.RenderingUtil
import com.intellij.util.Functions
import com.intellij.util.ui.JBUI
import java.awt.Cursor
import java.awt.Dimension
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.ListCellRenderer
import javax.swing.ListSelectionModel
import javax.swing.UIManager
import javax.swing.event.MouseInputAdapter

object ComboPopupSupport {
    /**
     * Creates a list component, possibly destined for the combobox popup.
     *
     * This factory only creates the list Swing component and requires to provide
     * both the items and the renderer.
     *
     * The width is computed from this ComboBox.
     *
     * Inspired by
     * [com.intellij.ui.popup.list.ListPopupImpl.createContent]
     *
     * @param renderer the renderer to use for the list
     * @param items the list of items to display
     * @param onClick the callback to invoke when an item is clicked
     * @return the component
     */
    fun <E> JComponent.makeChoicesList(
        renderer: ListCellRenderer<E>,
        items: List<E>,
        onClick: (list: JBList<E>, event: MouseEvent, value: E, index: Int) -> Unit,
        // = { _, _, value ->
        //     if (isSelectable(value)) {
        //         selectedItem = value
        //         customPopup?.cancel()
        //     }
        // },
    ): JList<E> {
        return JBList(items).apply {
            name = "dd.timeframe.options"
            cellRenderer = renderer
            selectionMode = ListSelectionModel.SINGLE_SELECTION
            selectionForeground = UIManager.getColor("ComboBox.selectionForeground")
            selectionBackground = UIManager.getColor("ComboBox.selectionBackground")

            visibleRowCount = itemsCount  // TODO make tweakable
            addMouseListener(object : MouseInputAdapter() {
                override fun mouseReleased(e: MouseEvent) {
                    if (model.size == 0 || selectedIndex < 0) return
                    onClick(this@apply, e, selectedValue, selectedIndex)
                }
            })
            border = JBUI.Borders.empty(PopupUtil.getListInsets(false, false))
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            preferredSize = Dimension(this@makeChoicesList.width, preferredSize.height)
            // Render the list as if it was focused
            // i.e., with a blue hover background in light/dark themes, instead of gray
            putClientProperty(RenderingUtil.ALWAYS_PAINT_SELECTION_AS_FOCUSED, true)
            putClientProperty(
                AnimatedIcon.ANIMATION_IN_RENDERER_ALLOWED,
                this@makeChoicesList.getClientProperty(AnimatedIcon.ANIMATION_IN_RENDERER_ALLOWED)
            )

            PopupUtil.applyNewUIBackground(this)
            ScrollingUtil.installActions(this)

            ListSpeedSearch.installOn(this, Functions.identity())
            ListHoverListener.DEFAULT.addTo(this)
        }
    }
}