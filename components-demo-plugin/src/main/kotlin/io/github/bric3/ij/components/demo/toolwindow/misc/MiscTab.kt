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

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.tabs.TabInfo
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.components.BorderLayoutPanel
import io.github.bric3.ij.components.ExpandableSplitter
import io.github.bric3.ij.components.icon.SvgIcon
import java.io.InputStream
import javax.swing.JLabel
import javax.swing.SwingConstants

class MiscTab : BorderLayoutPanel() {
    init {
        addToCenter(
            panel {
                svgIconGroup()
                expandableSplitter()
            }
        )
    }

    private fun Panel.expandableSplitter() {
        group("ExpandableSplitter") {
            row {
                cell(
                    ExpandableSplitter(
                        "ExpandableSplitter",
                        SwingConstants.LEFT,
                    ) {
                        val collapseAction: AnAction = getCollapseAction()
                        val expandableToolbar = ActionManager.getInstance().createActionToolbar(
                            "expandable",
                            DefaultActionGroup(
                                collapseAction,
                                DumbAwareAction.create(AllIcons.General.Gear) { }
                            ),
                            true
                        )
                        expandableComponent = BorderLayoutPanel()
                            .addToTop(expandableToolbar.component)
                            .addToCenter(JLabel("Side Content"))
                            .withBorder(JBUI.Borders.empty(3))
                        mainComponent = BorderLayoutPanel()
                            .addToCenter(JLabel("Main Content"))
                            .withBorder(JBUI.Borders.empty(3))
                    }
                ).align(Align.FILL)
            }.resizableRow()
        }.resizableRow()
    }

    private fun Panel.svgIconGroup() {
        group("SvgIcon from InputStream") {
            val s = "/icons/avocados-svgrepo-com.svg"
            row {
                icon(SvgIcon.fromStream(classpath(s)))
                icon(SvgIcon.fromStream(classpath("/icons/banana-svgrepo-com.svg")))
                icon(SvgIcon.fromStream(classpath("/icons/carrot-svgrepo-com.svg")))
                icon(SvgIcon.fromStream(classpath("/icons/cherry-svgrepo-com.svg")))
                icon(SvgIcon.fromStream(classpath("/icons/chilli-svgrepo-com.svg")))
                icon(SvgIcon.fromStream(classpath("/icons/eggplant-svgrepo-com.svg")))
                icon(SvgIcon.fromStream(classpath("/icons/food-svgrepo-com.svg")))
                icon(SvgIcon.fromStream(classpath("/icons/grapes-svgrepo-com.svg")))
                icon(SvgIcon.fromStream(classpath("/icons/mushrooms-svgrepo-com.svg")))
                icon(SvgIcon.fromStream(classpath("/icons/potatoes-svgrepo-com.svg")))
                icon(SvgIcon.fromStream(classpath("/icons/strawberry-svgrepo-com.svg")))
            }
            row {
                icon(SvgIcon.fromStream(classpath(s), 36))
                icon(SvgIcon.fromStream(classpath("/icons/banana-svgrepo-com.svg"), 36))
                icon(SvgIcon.fromStream(classpath("/icons/carrot-svgrepo-com.svg"), 36))
                icon(SvgIcon.fromStream(classpath("/icons/cherry-svgrepo-com.svg"), 36))
                icon(SvgIcon.fromStream(classpath("/icons/chilli-svgrepo-com.svg"), 36))
                icon(SvgIcon.fromStream(classpath("/icons/eggplant-svgrepo-com.svg"), 36))
                icon(SvgIcon.fromStream(classpath("/icons/food-svgrepo-com.svg"), 36))
                icon(SvgIcon.fromStream(classpath("/icons/grapes-svgrepo-com.svg"), 36))
                icon(SvgIcon.fromStream(classpath("/icons/mushrooms-svgrepo-com.svg"), 36))
                icon(SvgIcon.fromStream(classpath("/icons/potatoes-svgrepo-com.svg"), 36))
                icon(SvgIcon.fromStream(classpath("/icons/strawberry-svgrepo-com.svg"), 36))
            }
            row {
                icon(SvgIcon.fromStream(classpath(s), 10))
                icon(SvgIcon.fromStream(classpath("/icons/banana-svgrepo-com.svg"), 10))
                icon(SvgIcon.fromStream(classpath("/icons/carrot-svgrepo-com.svg"), 10))
                icon(SvgIcon.fromStream(classpath("/icons/cherry-svgrepo-com.svg"), 10))
                icon(SvgIcon.fromStream(classpath("/icons/chilli-svgrepo-com.svg"), 10))
                icon(SvgIcon.fromStream(classpath("/icons/eggplant-svgrepo-com.svg"), 10))
                icon(SvgIcon.fromStream(classpath("/icons/food-svgrepo-com.svg"), 10))
                icon(SvgIcon.fromStream(classpath("/icons/grapes-svgrepo-com.svg"), 10))
                icon(SvgIcon.fromStream(classpath("/icons/mushrooms-svgrepo-com.svg"), 10))
                icon(SvgIcon.fromStream(classpath("/icons/potatoes-svgrepo-com.svg"), 10))
                icon(SvgIcon.fromStream(classpath("/icons/strawberry-svgrepo-com.svg"), 10))
            }
        }
    }

    private fun classpath(s: String): InputStream = MiscTab::class.java.getResourceAsStream(s)

    companion object {
        val tabInfo
            get() = TabInfo(MiscTab()).apply {
                setText("Miscellaneous")
            }
    }
}