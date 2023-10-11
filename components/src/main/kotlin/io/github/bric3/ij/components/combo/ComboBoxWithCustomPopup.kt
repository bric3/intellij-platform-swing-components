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
import java.awt.Cursor
import java.awt.Dimension
import java.awt.MouseInfo
import java.awt.Point
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.JSeparator
import javax.swing.ListCellRenderer
import javax.swing.ListSelectionModel
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
                            content.addPropertyChangeListener("preferredSize") { _ ->
                                println("""
                                    ${Instant.now()} prefSize changed
                                      popup.size: ${it.size} 
                                      content.size: ${content.size}
                                      content.prefSize: ${content.preferredSize}
                                    """.trimIndent())
                                adjustPosition(it, content.preferredSize, true)
                            }

                            it.showUnderneathToTheRight(this@ComboBoxWithCustomPopup, content)
                        }
            } catch (e: Exception) {
                e.printStackTrace()
                return@invokeLater
            } finally {
                popupCreating.set(false)
            }
        }
    }

    private fun JBPopup.showUnderneathToTheRight(contextComponent: JComponent, content: JComponent) {
        // val popupWidth = content.preferredSize.width
        // val relativePoint = RelativePoint(contextComponent, Point(width - popupWidth, height))
        // this.show(relativePoint)

        adjustPosition(this, content.preferredSize)
    }

    private fun adjustPosition(popup: JBPopup, newSize: Dimension, checkResizing: Boolean = false) {
        // TODO does work well with our custom popup
        val positionAdjuster = PopupPositionManager.PositionAdjuster(this)
        val previousDimension = PopupPositionManager.PositionAdjuster.getPopupSize(popup)
        val adjustedBounds = positionAdjuster.adjustBounds(
            previousDimension, arrayOf(
                PopupPositionManager.Position.BOTTOM,
                PopupPositionManager.Position.RIGHT
            )
        )

        val popupSize = popup.size
        if (checkResizing && popupSize != null && adjustedBounds.height < 100) {
            setPopupVisible(false)
        } else {
            if (popup.canShow()) {
                val locationOnScreen = this.locationOnScreen
                println("""
                        ${Instant.now()} 
                          popup.size: $popupSize
                          newSize: $newSize
                          adjSize: $adjustedBounds
                          locationOnScreen: $locationOnScreen
                        """.trimIndent())

                popup.show(
                    RelativePoint(
                        this@ComboBoxWithCustomPopup,
                        Point(
                            this@ComboBoxWithCustomPopup.width - adjustedBounds.width,
                            adjustedBounds.y - locationOnScreen.y
                        )
                    )
                )
            } else {
                // Note: Setting the location before the size is important to avoid flickering
                // when the popup is expanded (on the left)
                // However, when the popup is reduced to its original size
                // then the popup still flickers.

                if (true) {
                    // TODO make the sticky side (LEFT or RIGHT) configurable
                    adjustedBounds.apply {
                        x = x + this@ComboBoxWithCustomPopup.width - newSize.width
                        width = newSize.width
                        height = newSize.height
                    }
                    println("""
                        ${Instant.now()} 
                          popup.size: $popupSize
                          newSize: $newSize
                          adjSize: $adjustedBounds
                        """.trimIndent())
                    // popup.setLocation(adjustedBounds.location)
                    //
                    // val size = adjustedBounds.size // for some reason this is less than newSize
                    // // val size = newSize
                    // if (newSize != popupSize) {
                    //     popup.size = size
                    // }

                    // TODO back to square one, the popup appears to be expanded first as the size
                    // grows, then changed location
                    // popup.setBounds(adjustedBounds)

                    // Use the window directly to set the bounds
                    val popupWindow = ComponentUtil.getWindow(popup.content) ?: return
                    println("${Instant.now()} popupWindow.bounds: ${popupWindow.bounds}")
                    // popupWindow.location = adjustedBounds.location
                    // popupWindow.size = adjustedBounds.size
                    popupWindow.bounds = popupWindow.bounds.apply {
                        this.location = adjustedBounds.location
                        this.size = adjustedBounds.size
                    }
                } else {
                    val size = adjustedBounds.size
                    if (size != previousDimension) {
                        popup.size = size
                    }

                    // TODO make the sticky side (LEFT or RIGHT) configurable
                    adjustedBounds.apply {
                        x = x + this@ComboBoxWithCustomPopup.width - popupSize.width
                    }
                    popup.setLocation(adjustedBounds.location)
                }
            }
        }
    }

/*
Open popup
==========

Breakpoint reached at com.datadog.intellij.context.ComboBoxCustomPopup$showCustomPopup$1$3$1.componentResized(ComboBoxCustomPopup.kt:226)
[2023-10-05T08:21:28.733368Z] java.awt.Dimension[width=242,height=280]
Breakpoint reached at com.datadog.intellij.context.ComboBoxCustomPopup.adjustPosition(ComboBoxCustomPopup.kt:286)
[2023-10-05T08:21:28.759665Z] java.awt.Rectangle[x=212,y=0,width=242,height=30]
Breakpoint reached at com.datadog.intellij.context.ComboBoxCustomPopup.adjustPosition(ComboBoxCustomPopup.kt:290)
[2023-10-05T08:21:28.814572Z] popup.size: java.awt.Dimension[width=242,height=280]
adjSize: java.awt.Dimension[width=242,height=280]
newSize: java.awt.Dimension[width=242,height=280]
prevDimension: java.awt.Dimension[width=242,height=280]


Hover more / Expand
===================
Breakpoint reached at com.datadog.intellij.context.TimeFramePopupPopupCreationContext$MoreHoverHelpVisibilityTrigger.onHover$lambda$1(Timeframe.kt:473)
[2023-10-05T08:21:39.653354Z] java.awt.Dimension[width=478,height=280]
Breakpoint reached at com.datadog.intellij.context.ComboBoxCustomPopup$showCustomPopup$1$3$1.componentResized(ComboBoxCustomPopup.kt:226)
[2023-10-05T08:21:39.667887Z] java.awt.Dimension[width=242,height=280]
Breakpoint reached at com.datadog.intellij.context.ComboBoxCustomPopup.adjustPosition(ComboBoxCustomPopup.kt:286)
[2023-10-05T08:21:39.672324Z] java.awt.Rectangle[x=212,y=0,width=242,height=30]
Breakpoint reached at com.datadog.intellij.context.ComboBoxCustomPopup.adjustPosition(ComboBoxCustomPopup.kt:290)
[2023-10-05T08:21:39.677113Z] popup.size: java.awt.Dimension[width=242,height=280]
adjSize: java.awt.Dimension[width=478,height=280]
newSize: java.awt.Dimension[width=242,height=280]
prevDimension: java.awt.Dimension[width=478,height=280]
Breakpoint reached at com.datadog.intellij.context.ComboBoxCustomPopup$showCustomPopup$1$3$1.componentResized(ComboBoxCustomPopup.kt:226)
[2023-10-05T08:21:39.683363Z] java.awt.Dimension[width=242,height=280]
Breakpoint reached at com.datadog.intellij.context.ComboBoxCustomPopup.adjustPosition(ComboBoxCustomPopup.kt:286)
[2023-10-05T08:21:39.687531Z] java.awt.Rectangle[x=212,y=0,width=242,height=30]
Breakpoint reached at com.datadog.intellij.context.ComboBoxCustomPopup.adjustPosition(ComboBoxCustomPopup.kt:290)
[2023-10-05T08:21:39.692019Z] popup.size: java.awt.Dimension[width=242,height=280]
adjSize: java.awt.Dimension[width=478,height=280]
newSize: java.awt.Dimension[width=242,height=280]
prevDimension: java.awt.Dimension[width=478,height=280]


Loosing focus / closing
=======================
Breakpoint reached at com.datadog.intellij.context.TimeFramePopupPopupCreationContext$MoreHoverHelpVisibilityTrigger.onHover$lambda$1(Timeframe.kt:473)
[2023-10-05T08:21:47.463775Z] java.awt.Dimension[width=478,height=280]
Breakpoint reached at com.datadog.intellij.context.ComboBoxCustomPopup$showCustomPopup$1$3$1.componentResized(ComboBoxCustomPopup.kt:226)
[2023-10-05T08:21:47.478630Z] java.awt.Dimension[width=478,height=280]
Breakpoint reached at com.datadog.intellij.context.ComboBoxCustomPopup.adjustPosition(ComboBoxCustomPopup.kt:286)
[2023-10-05T08:21:47.484846Z] java.awt.Rectangle[x=212,y=0,width=242,height=30]
Breakpoint reached at com.datadog.intellij.context.ComboBoxCustomPopup.adjustPosition(ComboBoxCustomPopup.kt:290)
[2023-10-05T08:21:47.492693Z] popup.size: java.awt.Dimension[width=242,height=280]
adjSize: java.awt.Dimension[width=478,height=280]
newSize: java.awt.Dimension[width=478,height=280]
prevDimension: java.awt.Dimension[width=478,height=280]

===========================================================================

Open popup
==========
Breakpoint reached at com.datadog.intellij.context.ComboBoxCustomPopup$showCustomPopup$1$3$1.componentResized(ComboBoxCustomPopup.kt:226)
[2023-10-05T08:26:28.547559Z] size: java.awt.Dimension[width=242,height=280]
prefSize: java.awt.Dimension[width=242,height=280]
Breakpoint reached at com.datadog.intellij.context.ComboBoxCustomPopup.adjustPosition(ComboBoxCustomPopup.kt:286)
[2023-10-05T08:26:28.573864Z] java.awt.Rectangle[x=212,y=0,width=242,height=30]
Breakpoint reached at com.datadog.intellij.context.ComboBoxCustomPopup.adjustPosition(ComboBoxCustomPopup.kt:290)
[2023-10-05T08:26:28.630258Z] popup.size: java.awt.Dimension[width=242,height=280]
adjSize: java.awt.Dimension[width=242,height=280]
newSize: java.awt.Dimension[width=242,height=280]
prevDimension: java.awt.Dimension[width=242,height=280]

Hover more / Expand
===================
Breakpoint reached at com.datadog.intellij.context.TimeFramePopupPopupCreationContext$MoreHoverHelpVisibilityTrigger.onHover$lambda$1(Timeframe.kt:473)
[2023-10-05T08:26:31.908400Z] java.awt.Dimension[width=478,height=280]
Breakpoint reached at com.datadog.intellij.context.ComboBoxCustomPopup$showCustomPopup$1$3$1.componentResized(ComboBoxCustomPopup.kt:226)
[2023-10-05T08:26:31.924166Z] size: java.awt.Dimension[width=242,height=280]
prefSize: java.awt.Dimension[width=478,height=280]
Breakpoint reached at com.datadog.intellij.context.ComboBoxCustomPopup.adjustPosition(ComboBoxCustomPopup.kt:286)
[2023-10-05T08:26:31.929990Z] java.awt.Rectangle[x=212,y=0,width=242,height=30]
Breakpoint reached at com.datadog.intellij.context.ComboBoxCustomPopup.adjustPosition(ComboBoxCustomPopup.kt:290)
[2023-10-05T08:26:31.935492Z] popup.size: java.awt.Dimension[width=242,height=280]
adjSize: java.awt.Dimension[width=478,height=280]
newSize: java.awt.Dimension[width=478,height=280]
prevDimension: java.awt.Dimension[width=478,height=280]
Breakpoint reached at com.datadog.intellij.context.ComboBoxCustomPopup$showCustomPopup$1$3$1.componentResized(ComboBoxCustomPopup.kt:226)
[2023-10-05T08:26:31.943181Z] size: java.awt.Dimension[width=242,height=280]
prefSize: java.awt.Dimension[width=478,height=280]
Breakpoint reached at com.datadog.intellij.context.ComboBoxCustomPopup.adjustPosition(ComboBoxCustomPopup.kt:286)
[2023-10-05T08:26:31.949494Z] java.awt.Rectangle[x=212,y=0,width=242,height=30]
Breakpoint reached at com.datadog.intellij.context.ComboBoxCustomPopup.adjustPosition(ComboBoxCustomPopup.kt:290)
[2023-10-05T08:26:31.955458Z] popup.size: java.awt.Dimension[width=242,height=280]
adjSize: java.awt.Dimension[width=478,height=280]
newSize: java.awt.Dimension[width=478,height=280]
prevDimension: java.awt.Dimension[width=478,height=280]


Mouse move?
===========
Breakpoint reached at com.datadog.intellij.context.TimeFramePopupPopupCreationContext$MoreHoverHelpVisibilityTrigger.onHover$lambda$1(Timeframe.kt:473)
[2023-10-05T08:26:32.191788Z] java.awt.Dimension[width=478,height=280]
Breakpoint reached at com.datadog.intellij.context.ComboBoxCustomPopup$showCustomPopup$1$3$1.componentResized(ComboBoxCustomPopup.kt:226)
[2023-10-05T08:26:32.201769Z] size: java.awt.Dimension[width=478,height=280]
prefSize: java.awt.Dimension[width=478,height=280]
Breakpoint reached at com.datadog.intellij.context.ComboBoxCustomPopup.adjustPosition(ComboBoxCustomPopup.kt:286)
[2023-10-05T08:26:32.208411Z] java.awt.Rectangle[x=212,y=0,width=242,height=30]
Breakpoint reached at com.datadog.intellij.context.ComboBoxCustomPopup.adjustPosition(ComboBoxCustomPopup.kt:290)
[2023-10-05T08:26:32.214469Z] popup.size: java.awt.Dimension[width=242,height=280]
adjSize: java.awt.Dimension[width=478,height=280]
newSize: java.awt.Dimension[width=478,height=280]
prevDimension: java.awt.Dimension[width=478,height=280]



*/

    fun updatePopupBounds(size: Dimension) {
        customPopup?.let {
            val content = it.content
            val newRelativeLocation = RelativePoint(this, Point(width - content.preferredSize.width, height))

            val popupWindow = ComponentUtil.getWindow(content) ?: return
            val bounds = popupWindow.bounds

            // calling #setLocation or #setSize makes the window move for a bit because of tricky computations
            // our aim here is to just move the window as-is to make it fit the screen
            // no tricks are included here
            popupWindow.bounds = bounds.apply {
                location = newRelativeLocation.screenPoint
                this.size = size
            }

            popupWindow.requestFocus()
            // content.size = size
            // content.revalidate()

            // it.size = size
            // it.setLocation(relativePoint.screenPoint)
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