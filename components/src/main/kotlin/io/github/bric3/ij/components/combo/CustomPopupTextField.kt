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

import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.observable.util.whenFocusGained
import com.intellij.openapi.observable.util.whenFocusLost
import com.intellij.openapi.observable.util.whenMousePressed
import com.intellij.openapi.observable.util.whenMouseReleased
import com.intellij.openapi.ui.addExtension
import com.intellij.openapi.ui.addKeyboardAction
import com.intellij.openapi.ui.getPreferredFocusedComponent
import com.intellij.openapi.ui.getUserData
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.openapi.ui.popup.util.PopupUtil
import com.intellij.openapi.ui.putUserData
import com.intellij.openapi.util.Key
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.fields.ExtendableTextField
import com.intellij.ui.popup.PopupPositionManager
import com.intellij.ui.popup.PopupPositionManager.Position
import io.github.bric3.ij.components.combo.CustomPopupTextField.PopupCreationContext
import java.awt.Dimension
import java.awt.MouseInfo
import java.awt.Point
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.KeyStroke

class CustomPopupTextField(
    private val popupContext: (parentTextField: CustomPopupTextField) -> PopupCreationContext,
) : ExtendableTextField() {
    val popup: PopupController = ReshapablePopupController(this, Position.RIGHT) { popupContext(this) }

    private var extClickedCount = 0

    fun interface PopupCreationContext {
        fun createPopupContent(): JComponent
    }

    init {
        addKeyboardAction(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0)) {
            popup.showPopup()
        }

        whenFocusGained {
            popup.showPopup()
        }

        whenFocusLost {
            popup.hidePopup()
        }

        // Note:
        //
        // 1. `whenMousePressed` listener is always called before extension action.
        // 2. `whenMouseReleased` listener is always called after extension action.
        //
        // That means that
        // * When clicking on the extension icon is the first interaction with this component,
        //   **the intention is to show the popup**,
        //   this is performed by `whenMousePressed`.
        //
        // * When clicking on the extension icon is not the first interaction with this component,
        //   **the popup is already shown and the intention is to hide it**, but `whenMousePressed`
        //   already ran, the intent is captured by counting the extension icon clicks,
        //   and the popup is hidden by `whenMouseReleased` if this count is greater than 0.

        whenMousePressed {
            extClickedCount = 0
            popup.showPopup()
        }

        whenMouseReleased {
            if (extClickedCount > 0) {
                popup.hidePopup()
            }
        }

        addExtension(
            AllIcons.General.ArrowDown,
            AllIcons.General.ArrowDown,
            null,
        ) {
            if (popup.isPopupVisible) {
                extClickedCount++
            }
        }
    }
}

interface PopupController {
    val isPopupVisible: Boolean
    fun showPopup()
    fun hidePopup()
    fun reshapePopup(newDimension: Dimension) // TODO is arg necessary?
}

class ReshapablePopupController(
    private val popupRequestor: CustomPopupTextField,
    private val anchor: Position = Position.RIGHT,
    private val popupContent: () -> PopupCreationContext,
) : PopupController {
    private var closeDueToReshaping: Boolean = false
    private var popup: JBPopup? = null

    init {
        require(anchor == Position.RIGHT || anchor == Position.LEFT) { "Only RIGHT or LEFT anchor is supported" }
    }
    
    override val isPopupVisible
        get() = popup?.isVisible == true

    override fun showPopup() {
        val popupContext = popupContent()
        val popupContent = popupContext.createPopupContent()

        PopupUtil.applyNewUIBackground(popupContent)
        if (popup == null || popup?.isVisible == false) {
            popup = createCustomPopup(
                popupContent,
                popupContent.getPreferredFocusedComponent()
            ).also {
                it.showUnderneathOf(anchor, popupRequestor)
            }
        }
    }

    override fun hidePopup() {
        popup?.cancel()
        popup = null
    }

    /**
     * Actually does not reshape the current popup but instead
     * creates a new one with the same content.
     */
    override fun reshapePopup(newSize: Dimension) {
        val oldPopup = popup ?: return
        if (oldPopup.isDisposed) return

        val popupContent = oldPopup.content.getUserData(POPUP_CONTENT)!!
        val focusOwner = IdeFocusManager.findInstance().focusOwner as? JComponent
            ?: popupContent.getPreferredFocusedComponent()

        // Try first to unmount popupContent from the old popup
        popupContent.parent.remove(popupContent)
        // Schedule hide of the old popup
        ApplicationManager.getApplication().invokeLater {
            closeDueToReshaping = true
            oldPopup.cancel()
        }

        popup = createCustomPopup(
            popupContent,
            focusOwner,
        ).also {
            it.showUnderneathOf(anchor, popupRequestor)
        }
        // synthetic mouse move event so that component
        // that reacted before to a previous mouse event
        // can react again to the new mouse event
        val mouseLocation = MouseInfo.getPointerInfo().location
        popupContent.dispatchEvent(
            MouseEvent(
                popupContent,
                MouseEvent.MOUSE_MOVED,
                System.currentTimeMillis(),
                0,
                mouseLocation.x,
                mouseLocation.y,
                0,
                false
            )
        )
    }

    private fun createCustomPopup(
        popupContent: JComponent,
        preferredFocusableComponent: JComponent?,
    ) = JBPopupFactory.getInstance()
        .createComponentPopupBuilder(
            popupContent,
            preferredFocusableComponent
        )
        .setRequestFocus(false) // If true there's a strange effect when clicking outside the popup
        .setFocusable(true)
        .setMovable(false)
        .setCancelKeyEnabled(true)
        .setLocateByContent(false)
        .setLocateWithinScreenBounds(true)
        .setModalContext(false)
        .setMayBeParent(true) // this creates a popup as a dialog with alwaysOnTop=false
        .setShowShadow(true)
        .setShowBorder(true)
        .setCancelOnWindowDeactivation(true)
        // canceled on focus lost
        .addListener(object : JBPopupListener {
            override fun beforeShown(event: LightweightWindowEvent) {
                // Possibly install similar mouse listeners as ListPopupImpl
                // com.intellij.ui.popup.list.ListPopupImpl.beforeShow
            }

            override fun onClosed(event: LightweightWindowEvent) {
                // don't call this listener when reshaping
                if (closeDueToReshaping) {
                    return
                }
                popup = null
                closeDueToReshaping = false
            }
        })
        .createPopup()
        .also {
            it.content.putUserData(POPUP_CONTENT, popupContent)
        }

    private fun JBPopup.showUnderneathOf(anchor: Position, componentUnder: JComponent) {
        when (anchor) {
            Position.RIGHT -> showUnderneathToTheRight(componentUnder)
            Position.LEFT -> showUnderneathOf(componentUnder) // TODO fix location
            else -> {}
        }
    }

    private fun JBPopup.showUnderneathToTheRight(componentUnder: JComponent) {
        showOrAdjustPosition(this, componentUnder)
    }

    private fun showOrAdjustPosition(popup: JBPopup, componentUnder: JComponent, checkResizing: Boolean = false) {
        val positionAdjuster = PopupPositionManager.PositionAdjuster(componentUnder)
        val previousDimension = popupSize(popup)
        val adjustedBounds = positionAdjuster.adjustBounds(
            previousDimension,
            arrayOf(
                Position.BOTTOM,
                Position.RIGHT
            )
        )

        val popupSize = popup.size
        if (checkResizing && popupSize != null && adjustedBounds.height < 100) {
            showPopup()
        } else {
            check(popup.canShow())
            val locationOnScreen = componentUnder.locationOnScreen
            popup.show(
                RelativePoint(
                    componentUnder,
                    Point(
                        componentUnder.width - previousDimension.width,
                        adjustedBounds.y - locationOnScreen.y
                    )
                )
            )
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

    companion object {
        val POPUP_CONTENT = Key.create<JComponent>(this::class.simpleName!! + ".POPUP_CONTENT")
    }
}