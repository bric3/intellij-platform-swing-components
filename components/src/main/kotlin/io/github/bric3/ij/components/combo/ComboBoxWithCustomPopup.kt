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
package io.github.bric3.ij.components.combo

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.openapi.ui.popup.util.PopupUtil
import com.intellij.openapi.util.Disposer
import com.intellij.ui.AnimatedIcon.ANIMATION_IN_RENDERER_ALLOWED
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.ComponentUtil
import com.intellij.ui.ListSpeedSearch
import com.intellij.ui.PopupMenuListenerAdapter
import com.intellij.ui.ScrollingUtil
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.TitledSeparator
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBList
import com.intellij.ui.hover.ListHoverListener
import com.intellij.ui.popup.PopupPositionManager
import com.intellij.ui.render.RenderingUtil
import com.intellij.util.ArrayUtil
import com.intellij.util.Functions
import com.intellij.util.ui.JBUI
import java.awt.Component
import java.awt.Container
import java.awt.Cursor
import java.awt.Dimension
import java.awt.MouseInfo
import java.awt.Point
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Consumer
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.JRootPane
import javax.swing.JSeparator
import javax.swing.ListCellRenderer
import javax.swing.ListSelectionModel
import javax.swing.RepaintManager
import javax.swing.SwingUtilities
import javax.swing.event.MouseInputAdapter
import javax.swing.event.PopupMenuEvent

abstract class ComboBoxWithCustomPopup<T>(model: CollectionComboBoxModel<T>) :
        ComboBox<T>(model) {
    private var customPopup: JBPopup? = null
    private var popupCreating = AtomicBoolean(false)
    private var popupNeedsCancel = AtomicBoolean(false)

    init {
        // TODO handle up/down arrow navigation
        @Suppress("LeakingThis")
        addPopupMenuListener(object : PopupMenuListenerAdapter() {
            override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) {
                showCustomPopup()
            }
        })

        editor.editorComponent.run {
            addMouseListener(object : MouseInputAdapter() {
                override fun mousePressed(e: MouseEvent?) {
                    showCustomPopup()
                }
            })

            addFocusListener(object : FocusListener {
                override fun focusGained(e: FocusEvent) {
                    showCustomPopup()
                }

                override fun focusLost(e: FocusEvent) {
                    customPopup?.cancel()
                }
            })
        }

        // The default ComboBox renderer does not produce the hovered backbround,
        // hence defining one is necessary.
        renderer = SimpleListCellRenderer.create<T>("", Functions.identity())
    }

    override fun updateUI() {
        super.updateUI()
        // Tweak default listeners like BasicComboPopup.Handler to avoid the mousePressed.
        // Unfortunately, this is necessary because the installed listener by `BasicComboUI`
        // is automatically installed on the comboBox and the arrow button, and it directly
        // uses `BasicComboPopup` API.
        fun patchMousePressed(component: Component) {
            // val handlerClass = Class.forName("javax.swing.plaf.basic.BasicComboPopup\$Handler")
            // val comboBoxPopupML = component.mouseListeners.filterIsInstance(handlerClass).first() as MouseListener
            val comboBoxPopupML = ArrayUtil.getLastElement(component.mouseListeners)
            component.removeMouseListener(comboBoxPopupML)
            component.addMouseListener(
                    object : MouseListener by comboBoxPopupML {
                        // Source code from `BasicComboPopup.Handler.mousePressed`
                        // but replaces the method that show popup by our own `showCustomPopup`
                        // This listener is necessary to maintain focus on the combobox,
                        // especially in the New UI otherwise when the component looses focus
                        // the owning action toolbar is turned invisible.
                        override fun mousePressed(e: MouseEvent) {
                            if (!SwingUtilities.isLeftMouseButton(e) || !this@ComboBoxWithCustomPopup.isEnabled) return

                            if (this@ComboBoxWithCustomPopup.isEditable()) {
                                val comp = this@ComboBoxWithCustomPopup.editor.editorComponent
                                if (comp !is JComponent || comp.isRequestFocusEnabled) {
                                    comp.requestFocus()
                                }
                            } else if (this@ComboBoxWithCustomPopup.isRequestFocusEnabled) {
                                this@ComboBoxWithCustomPopup.requestFocus()
                            }
                            if (isPopupVisible) {
                                popupNeedsCancel.set(true)
                                customPopup?.cancel(e)
                            } else {
                                showCustomPopup()
                            }
                        }
                    }
            )
        }
        patchMousePressed(this)
        components.forEach(::patchMousePressed)
    }

    protected abstract fun getPopupCreationContext(parentDisposable: Disposable): PopupCreationContext

    override fun showPopup() {
        showCustomPopup()
    }

    override fun isPopupVisible(): Boolean {
        return customPopup?.isVisible ?: false
    }

    override fun setPopupVisible(visible: Boolean) {
        super.setPopupVisible(visible)
        if (visible) {
            showCustomPopup()
        } else {
            if (customPopup?.isDisposed == false) {
                customPopup?.cancel()
            }
        }
    }

    protected fun showCustomPopup() {
        hidePopup() // hide the default combobox popup
        val visible = customPopup?.isVisible ?: false
        if (popupCreating.getAndSet(true) || visible) {
            return
        }

        val popupDisposable = Disposer.newDisposable("ComboBoxCustomPopup")

        invokeLater {
            // For some reason™️, the default popup might still try to show.
            hidePopup()

            if (customPopup != null) {
                // This check happens on a different EDT thread, needs to check this again
                @Suppress("KotlinConstantConditions")
                if (visible) {
                    return@invokeLater
                }
                customPopup?.cancel()
            }

            try {
                val popupContext = getPopupCreationContext(parentDisposable = popupDisposable)
                val content = popupContext.createPopupContent()

                PopupUtil.applyNewUIBackground(content)

                // Creates a popup with the same setting as
                customPopup =
                        JBPopupFactory.getInstance()
                                .createComponentPopupBuilder(
                                        content,
                                        popupContext.getPreferredFocusableComponent()
                                )
                                .setRequestFocus(false) // If true there's a strange effect when clicking outside the popup
                                .setFocusable(true)
                                .setMovable(false)
                                .setCancelOnClickOutside(true)
                                .setCancelOnOtherWindowOpen(true)
                                .setCancelKeyEnabled(true)
                                .setLocateByContent(false)
                                .setLocateWithinScreenBounds(true)
                                .setModalContext(false)
                                .setMayBeParent(true) // this creates a popup as a dialog with alwaysOnTop=false
                                .setShowShadow(true)
                                .setShowBorder(true)
                                .setCancelOnWindowDeactivation(true)
                                .setCancelCallback {
                                    if (popupNeedsCancel.getAndSet(false)) {
                                        return@setCancelCallback true
                                    }
                                    // If the popup owner is not visible or not showing, then cancel is allowed
                                    if (!this@ComboBoxWithCustomPopup.isVisible || !this@ComboBoxWithCustomPopup.isShowing) {
                                        return@setCancelCallback true
                                    }
                                    // If the popup is shown and the pointer is
                                    // not within the bounds of the combobox, then cancel is allowed
                                    val mousePos: Point = MouseInfo.getPointerInfo().location
                                    val bounds = this@ComboBoxWithCustomPopup.bounds.apply {
                                        location = locationOnScreen
                                    }
                                    return@setCancelCallback !bounds.contains(mousePos)
                                }
                                // .setCancelOnMouseOutCallback {
                                //     true
                                // }
                                .addListener(object : JBPopupListener {
                                    override fun beforeShown(event: LightweightWindowEvent) {
                                        // Possibly install similar mouse listeners as ListPopupImpl
                                        // com.intellij.ui.popup.list.ListPopupImpl.beforeShow
                                    }

                                    override fun onClosed(event: LightweightWindowEvent) {
                                        Disposer.dispose(popupDisposable)
                                        customPopup = null
                                        popupCreating.set(false)
                                    }
                                })
                                .createPopup().also {
                                    it.showUnderneathToTheRight()
                                }
            } catch (e: Exception) {
                e.printStackTrace()
                return@invokeLater
            } finally {
                popupCreating.set(false)
            }
        }
    }

    private fun JBPopup.showUnderneathToTheRight() {
        showOrAdjustPosition(this)
    }

    private fun showOrAdjustPosition(popup: JBPopup, checkResizing: Boolean = false) {
        // TODO does work well with our custom popup
        val positionAdjuster = PopupPositionManager.PositionAdjuster(this@ComboBoxWithCustomPopup)
        val previousDimension = popupSize(popup)
        val adjustedBounds = positionAdjuster.adjustBounds(
                previousDimension,
                arrayOf(
                        PopupPositionManager.Position.BOTTOM,
                        PopupPositionManager.Position.RIGHT
                )
        )

        val popupSize = popup.size
        if (checkResizing && popupSize != null && adjustedBounds.height < 100) {
            setPopupVisible(false)
        } else {
            if (popup.canShow()) {
                val locationOnScreen = this@ComboBoxWithCustomPopup.locationOnScreen
                popup.show(
                        RelativePoint(
                                this@ComboBoxWithCustomPopup,
                                Point(
                                        this@ComboBoxWithCustomPopup.width - previousDimension.width,
                                        adjustedBounds.y - locationOnScreen.y
                                )
                        )
                )
            } else {
                // Note: Setting the location before the size is important to avoid flickering
                // when the popup is expanded (on the left)
                // However, when the popup is reduced to its original size
                // then the popup still flickers.

                // TODO make the sticky side (LEFT or RIGHT) configurable
                adjustedBounds.apply {
                    x = x + this@ComboBoxWithCustomPopup.width - adjustedBounds.width
                    width = adjustedBounds.width
                    height = adjustedBounds.height
                }

                // Use the window directly to set the bounds
                val popupWindow = ComponentUtil.getWindow(popup.content) ?: return

                popupWindow.invalidate()
                RepaintManager.currentManager(popupWindow).markCompletelyDirty(popup.content)

                // bounds approach
                popupWindow.setBounds(
                        adjustedBounds.x,
                        adjustedBounds.y,
                        adjustedBounds.width,
                        adjustedBounds.height
                )
            }
        }
    }

    private fun forEachParentComponent(container: Container, consumer: Consumer<in Component>) {
        var c: Container? = container
        while (c != null) {
            consumer.accept(c)
            c = c.parent
            if (c is JRootPane) {
                break
            }
        }
    }

    /**
     * Loose equivalent of [PopupPositionManager.PositionAdjuster.getPopupSize]
     *
     * THis method has been removed by mistake and moved to `PopupImplUtil` sometime
     * during 2023.3 development, see this
     * [commit](https://github.com/JetBrains/intellij-community/commit/edf3db21b1977bfd5b9585719ef649d7ad454971),
     * this method was later added back in this [commit](https://github.com/JetBrains/intellij-community/commit/40f3c5aac846c39fe4aded0fe9f5243d2e03df7c) on `master` and `233` branches.
     *
     * See [IDEA-339764](https://youtrack.jetbrains.com/issue/IDEA-339764)
     */
    // Replace this method once based on 241 by PopupPositionManager.PositionAdjuster.getPopupSize
    private fun popupSize(popup: JBPopup): Dimension {
        return popup.content.preferredSize
    }

    fun updatePopupBounds(newSize: Dimension) {
        customPopup?.let {
            if (it.isDisposed) return

            showOrAdjustPosition(it, true)
        }
    }

    protected fun customPopup(): JBPopup? {
        return customPopup
    }

    interface PopupCreationContext {
        fun createPopupContent(): JComponent
        fun getPreferredFocusableComponent(): JComponent? = null
    }

    // see com.intellij.ui.popup.list.ComboBoxPopup.MyDelegateRenderer
    class ComboChoicesListRenderer<E>(private val comboBox: ComboBox<E>) : ListCellRenderer<E> {
        override fun getListCellRendererComponent(
                list: JList<out E>,
                value: E,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean
        ): Component {
            val component = comboBox.renderer.getListCellRendererComponent(
                    list,
                    value,
                    index,
                    isSelected,
                    cellHasFocus
            )
            if (component is JComponent && !(component is JSeparator || component is TitledSeparator)) {
                component.setBorder(JBUI.Borders.empty(2, 8))
            }
            return component
        }
    }

    fun isSelectable(item: T): Boolean {
        for (i in 0 until model.size) {
            if (model.getElementAt(i) == item) {
                return true
            }
        }
        return false
    }

    companion object {
        /**
         * Creates a list from this ComboBox choices, possibly destined for the combobox popup.
         *
         * @return the component
         */
        fun <E> ComboBoxWithCustomPopup<E>.makeComboBoxList(): JComponent {
            return makeChoicesList(
                    renderer = ComboChoicesListRenderer(comboBox = this),
                    items = buildList {
                        for (i in 0 until model.size) {
                            add(model.getElementAt(i))
                        }
                    }
            )
        }

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
        fun <E> ComboBoxWithCustomPopup<E>.makeChoicesList(
                renderer: ListCellRenderer<E>,
                items: List<E>,
                onClick: (list: JBList<E>, index: Int, value: E) -> Unit = { _, _, value ->
                    if (isSelectable(value)) {
                        selectedItem = value
                        customPopup?.cancel()
                    }
                },
        ): JComponent {
            return JBList(items).apply {
                name = "dd.timeframe.options"
                cellRenderer = renderer
                selectionMode = ListSelectionModel.SINGLE_SELECTION
                visibleRowCount = itemsCount
                addMouseListener(object : MouseInputAdapter() {
                    override fun mouseReleased(e: MouseEvent) {
                        if (model.size == 0) return
                        onClick(this@apply, selectedIndex, selectedValue)
                    }
                })
                border = JBUI.Borders.empty()
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                preferredSize = Dimension(this@makeChoicesList.width, preferredSize.height)
                // Render the list as if it was focused
                // i.e., with a blue hover background in light/dark themes, instead of gray
                putClientProperty(RenderingUtil.ALWAYS_PAINT_SELECTION_AS_FOCUSED, true)
                putClientProperty(
                        ANIMATION_IN_RENDERER_ALLOWED,
                        this@makeChoicesList.getClientProperty(ANIMATION_IN_RENDERER_ALLOWED)
                )

                PopupUtil.applyNewUIBackground(this)
                ScrollingUtil.installActions(this)
                // 232 Use TreeUIHelper.installListSpeedSearch(javax.swing.JList<T>, com.intellij.util.containers.Convertor<? super T,java.lang.String>)
                ListSpeedSearch(this, Functions.identity())
                ListHoverListener.DEFAULT.addTo(this)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun getSelectedItem(): T? {
        return super.getSelectedItem() as T?
    }
}