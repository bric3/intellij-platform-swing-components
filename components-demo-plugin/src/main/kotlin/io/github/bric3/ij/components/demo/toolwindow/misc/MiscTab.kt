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
package io.github.bric3.ij.components.demo.toolwindow.misc

import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.tabs.TabInfo
import com.intellij.util.ui.components.BorderLayoutPanel
import io.github.bric3.ij.components.icon.SvgIcon

class MiscTab : BorderLayoutPanel() {
    init {
        addToCenter(
            panel {
                group("SvgIcon from inputstream") {
                    row {
                        icon(SvgIcon.fromStream(MiscTab::class.java.getResourceAsStream("/icons/avocados-svgrepo-com.svg")))
                        icon(SvgIcon.fromStream(MiscTab::class.java.getResourceAsStream("/icons/banana-svgrepo-com.svg")))
                        icon(SvgIcon.fromStream(MiscTab::class.java.getResourceAsStream("/icons/carrot-svgrepo-com.svg")))
                        icon(SvgIcon.fromStream(MiscTab::class.java.getResourceAsStream("/icons/cherry-svgrepo-com.svg")))
                        icon(SvgIcon.fromStream(MiscTab::class.java.getResourceAsStream("/icons/chilli-svgrepo-com.svg")))
                        icon(SvgIcon.fromStream(MiscTab::class.java.getResourceAsStream("/icons/eggplant-svgrepo-com.svg")))
                        icon(SvgIcon.fromStream(MiscTab::class.java.getResourceAsStream("/icons/food-svgrepo-com.svg")))
                        icon(SvgIcon.fromStream(MiscTab::class.java.getResourceAsStream("/icons/grapes-svgrepo-com.svg")))
                        icon(SvgIcon.fromStream(MiscTab::class.java.getResourceAsStream("/icons/mushrooms-svgrepo-com.svg")))
                        icon(SvgIcon.fromStream(MiscTab::class.java.getResourceAsStream("/icons/potatoes-svgrepo-com.svg")))
                        icon(SvgIcon.fromStream(MiscTab::class.java.getResourceAsStream("/icons/strawberry-svgrepo-com.svg")))
                    }
                    row {
                        icon(SvgIcon.fromStream(MiscTab::class.java.getResourceAsStream("/icons/avocados-svgrepo-com.svg"), 36))
                        icon(SvgIcon.fromStream(MiscTab::class.java.getResourceAsStream("/icons/banana-svgrepo-com.svg"), 36))
                        icon(SvgIcon.fromStream(MiscTab::class.java.getResourceAsStream("/icons/carrot-svgrepo-com.svg"), 36))
                        icon(SvgIcon.fromStream(MiscTab::class.java.getResourceAsStream("/icons/cherry-svgrepo-com.svg"), 36))
                        icon(SvgIcon.fromStream(MiscTab::class.java.getResourceAsStream("/icons/chilli-svgrepo-com.svg"), 36))
                        icon(SvgIcon.fromStream(MiscTab::class.java.getResourceAsStream("/icons/eggplant-svgrepo-com.svg"), 36))
                        icon(SvgIcon.fromStream(MiscTab::class.java.getResourceAsStream("/icons/food-svgrepo-com.svg"), 36))
                        icon(SvgIcon.fromStream(MiscTab::class.java.getResourceAsStream("/icons/grapes-svgrepo-com.svg"), 36))
                        icon(SvgIcon.fromStream(MiscTab::class.java.getResourceAsStream("/icons/mushrooms-svgrepo-com.svg"), 36))
                        icon(SvgIcon.fromStream(MiscTab::class.java.getResourceAsStream("/icons/potatoes-svgrepo-com.svg"), 36))
                        icon(SvgIcon.fromStream(MiscTab::class.java.getResourceAsStream("/icons/strawberry-svgrepo-com.svg"), 36))
                    }
                    row {
                        icon(SvgIcon.fromStream(MiscTab::class.java.getResourceAsStream("/icons/avocados-svgrepo-com.svg"), 10))
                        icon(SvgIcon.fromStream(MiscTab::class.java.getResourceAsStream("/icons/banana-svgrepo-com.svg"), 10))
                        icon(SvgIcon.fromStream(MiscTab::class.java.getResourceAsStream("/icons/carrot-svgrepo-com.svg"), 10))
                        icon(SvgIcon.fromStream(MiscTab::class.java.getResourceAsStream("/icons/cherry-svgrepo-com.svg"), 10))
                        icon(SvgIcon.fromStream(MiscTab::class.java.getResourceAsStream("/icons/chilli-svgrepo-com.svg"), 10))
                        icon(SvgIcon.fromStream(MiscTab::class.java.getResourceAsStream("/icons/eggplant-svgrepo-com.svg"), 10))
                        icon(SvgIcon.fromStream(MiscTab::class.java.getResourceAsStream("/icons/food-svgrepo-com.svg"), 10))
                        icon(SvgIcon.fromStream(MiscTab::class.java.getResourceAsStream("/icons/grapes-svgrepo-com.svg"), 10))
                        icon(SvgIcon.fromStream(MiscTab::class.java.getResourceAsStream("/icons/mushrooms-svgrepo-com.svg"), 10))
                        icon(SvgIcon.fromStream(MiscTab::class.java.getResourceAsStream("/icons/potatoes-svgrepo-com.svg"), 10))
                        icon(SvgIcon.fromStream(MiscTab::class.java.getResourceAsStream("/icons/strawberry-svgrepo-com.svg"), 10))
                    }
                }
            }
        )
    }
    companion object {
        val tabInfo
            get() = TabInfo(MiscTab()).apply {
                setText("Miscellaneous")
            }
    }
}