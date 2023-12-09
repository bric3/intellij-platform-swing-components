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

import com.intellij.ide.ui.laf.darcula.ui.DarculaProgressBarUI
import java.awt.Color
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import javax.swing.JComponent
import javax.swing.JProgressBar

class ColoredJProgressBar(min: Int, max: Int) : JProgressBar(HORIZONTAL, min, max) {
    var remainderColor: Color? = null
    var finishedColor: Color? = null
    var ratio: Double
        get() = value.toDouble() / model.maximum
        set(ratio) {
            value = (ratio * model.maximum).toInt()
        }

    init {
        isOpaque = false
        // '3' is the lower limit to see rounded ends on non-hdpi screens,
        // this value is scaled by
        // com.intellij.ide.ui.laf.darcula.ui.DarculaProgressBarUI.getStripeWidth
        // upon getPreferredSize()
        putClientProperty("ProgressBar.stripeWidth", 3)
        putClientProperty("ProgressBar.flatEnds", java.lang.Boolean.FALSE)
    }

    fun adjustPreferredWidth(width: Int): ColoredJProgressBar {
        preferredSize = preferredSize.apply {
            this.width = width
        } // setter needed to adjust the width, kotlin will use the setter
        return this
    }

    fun adjustMaximumWidth(width: Int): ColoredJProgressBar {
        maximumSize = preferredSize.apply {
            this.width = width
        } // setter needed to adjust the width, kotlin will use the setter
        return this
    }

    override fun updateUI() {
        // reset preferred size, so it's computed again on updated UI
        // sucha as toggling on/off presentation mode
        preferredSize = null

        // As this component is used to display gauges, this also prevents
        // visual plugin extension like the infamous nyan cat plugin to interfere
        // with the UI of this gauge component
        this.setUI(ColoredDarculaProgressBarUI(this))
    }

    class ColoredDarculaProgressBarUI(
        private val coloredJProgressBar: ColoredJProgressBar
    ) : DarculaProgressBarUI() {
        override fun getRemainderColor(): Color {
            return when (val customRemainderColor = coloredJProgressBar.remainderColor) {
                null -> super.getRemainderColor()
                else -> customRemainderColor
            }
        }

        override fun getFinishedColor(c: JComponent?): Color {
            return when (val customFinishedColor = coloredJProgressBar.finishedColor) {
                null -> superGetFinishedColor(c)
                else -> customFinishedColor
            }
        }

        @Suppress("unused") // for older than 231
        fun getFinishedColor(): Color {
            return getFinishedColor(null)
        }

        private fun superGetFinishedColor(component: JComponent?): Color {
            val revealDirect = MethodHandles.lookup().revealDirect(super_getFinishedColor_MH)
            val finishedColor = if (revealDirect.methodType.parameterArray()[0] == JComponent::class.java) {
                // post 231
                super_getFinishedColor_MH.invoke(this, component) as Color
            } else {
                // pre 231
                super_getFinishedColor_MH.invoke(component) as Color
            }
            return finishedColor
        }

        // Handle API change in 231, getFinishedColor now takes a JComponent
        @Suppress("PrivatePropertyName")
        private val super_getFinishedColor_MH = try {
            MethodHandles.privateLookupIn(DarculaProgressBarUI::class.java, MethodHandles.lookup())
                .findSpecial(
                    DarculaProgressBarUI::class.java,
                    "getFinishedColor",
                    MethodType.methodType(Color::class.java, JComponent::class.java),
                    DarculaProgressBarUI::class.java
                )
        } catch (e: NoSuchMethodError) {
            try {
                MethodHandles.privateLookupIn(DarculaProgressBarUI::class.java, MethodHandles.lookup())
                    .findSpecial(
                        DarculaProgressBarUI::class.java,
                        "getFinishedColor",
                        MethodType.methodType(Color::class.java),
                        DarculaProgressBarUI::class.java
                    )
            } catch (e: NoSuchFieldException) {

                throw IllegalStateException(
                    "Fix to recalculate row height on updateUI (like switching to/from presentation mode) broken, please update",
                    e

                )
            }
        }
    }
}