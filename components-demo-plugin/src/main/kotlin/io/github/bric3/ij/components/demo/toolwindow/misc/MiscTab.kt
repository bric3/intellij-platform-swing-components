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
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.observable.properties.AtomicLazyProperty
import com.intellij.openapi.observable.util.bindVisible
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.popup.util.PopupUtil
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.hover.ListHoverListener
import com.intellij.ui.tabs.TabInfo
import com.intellij.ui.util.preferredWidth
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.components.BorderLayoutPanel
import io.github.bric3.ij.components.ExpandableSplitter
import io.github.bric3.ij.components.combo.ComboBoxWithCustomPopup
import io.github.bric3.ij.components.combo.ComboBoxWithCustomPopup.Companion.makeComboBoxList
import io.github.bric3.ij.components.demo.toolwindow.DemoToolWindowFactory
import io.github.bric3.ij.components.icon.SvgIcon
import java.awt.Dimension
import java.io.InputStream
import javax.annotation.Priority
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.SwingConstants

class MiscTab : BorderLayoutPanel() {
    init {
        addToCenter(
            panel {
                customPopupComboBox()
                svgIconGroup()
                expandableSplitter()
            }
        )
    }

    private fun Panel.customPopupComboBox() {
        group("Custom combobox popup") {
            row {
                val values = mapOf(
                    "a" to "details about a",
                    "b" to "details about b",
                    "c" to "details about c",
                )

                cell(
                    object : ComboBoxWithCustomPopup<String>(CollectionComboBoxModel(values.keys.sorted())) {
                        override fun getPopupCreationContext(parentDisposable: Disposable): PopupCreationContext {
                            return MyPopupCreationContext(this, values)
                        }
                    }
                )
            }
        }
    }

    class MyPopupCreationContext(private val combo: ComboBoxWithCustomPopup<String>, private val values: Map<String, String>) :
        ComboBoxWithCustomPopup.PopupCreationContext {
        private val detailsVisibility = AtomicLazyProperty { false }
        private val hoveredDetail = AtomicLazyProperty { "" }
        private val details = panel {
            group("Details:") {
                row {
                    label("").bindText(hoveredDetail)
                }
            }
        }.bindVisible(detailsVisibility).withMinimumWidth(150).withPreferredWidth(150)

        private val list = combo.makeComboBoxList().apply {
            object : ListHoverListener() {
                override fun onHover(list: JList<*>, index: Int) {
                    hoveredDetail.set(
                        when (index) {
                            -1 -> ""
                            else -> values[list.model.getElementAt(index) as String] ?: ""
                        }
                    )
                }
            }.addTo(this)
        }
        override fun getPreferredFocusableComponent() = list

        override fun createPopupContent(): JComponent {
            return BorderLayoutPanel().apply {
                addToLeft(details.apply { isVisible = false })
                addToCenter(BorderLayoutPanel()
                    .addToCenter(list)
                    .addToBottom(panel {
                        row {
                            checkBox("Show details").bindSelected(detailsVisibility)
                        }
                    })
                )

                // Applying the border here, instead of content
                // to mimic other popup lists.
                border = JBUI.Borders.empty(PopupUtil.getListInsets(false, false))
                PopupUtil.applyNewUIBackground(this)

                detailsVisibility.afterChange {
                    this.preferredSize = if (it)
                        Dimension(list.width + details.preferredWidth, this.height)
                    else
                        Dimension(list.width, this.height)
                }
            }
        }
    }

    private fun Panel.expandableSplitter() {
        group("ExpandableSplitter") {
            row {
                cell(
                    ExpandableSplitter(
                        "ExpandableSplitter",
                        SwingConstants.LEFT,
                    ) {
                        val expandableToolbar = ActionManager.getInstance().createActionToolbar(
                            "expandable",
                            DefaultActionGroup(
                                getCollapseAction(),
                                DumbAwareAction.create(AllIcons.General.Gear) { }
                            ),
                            false
                        )

                        expandableComponent = BorderLayoutPanel()
                            .addToLeft(expandableToolbar.component)
                            .addToCenter(JLabel("Side Content"))
                            .withBorder(JBUI.Borders.empty(3))
                        expandableToolbar.targetComponent = expandableComponent

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

    @Priority(3)
    class Factory : DemoToolWindowFactory.TabFactory {
        override fun createTab() = TabInfo(MiscTab()).apply {
            setText("Miscellaneous")
        }
    }
}