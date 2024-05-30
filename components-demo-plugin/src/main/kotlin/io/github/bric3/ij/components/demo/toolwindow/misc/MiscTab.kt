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
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.impl.ActionButtonWithText
import com.intellij.openapi.editor.impl.EditorCssFontResolver
import com.intellij.openapi.observable.properties.AtomicLazyProperty
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.observable.util.bindVisible
import com.intellij.openapi.observable.util.transform
import com.intellij.openapi.observable.util.trim
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.DumbAwareToggleAction
import com.intellij.openapi.ui.popup.util.PopupUtil
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.JBColor
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.UnscaledGaps
import com.intellij.ui.hover.ListHoverListener
import com.intellij.ui.tabs.TabInfo
import com.intellij.ui.util.preferredWidth
import com.intellij.util.ui.ExtendableHTMLViewFactory
import com.intellij.util.ui.HTMLEditorKitBuilder
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import io.github.bric3.ij.components.ExpandableSplitter
import io.github.bric3.ij.components.combo.ComboBoxWithCustomPopup
import io.github.bric3.ij.components.combo.ComboBoxWithCustomPopup.Companion.makeComboBoxList
import io.github.bric3.ij.components.demo.toolwindow.DemoToolWindowFactory
import io.github.bric3.ij.components.dsl.actionLink
import io.github.bric3.ij.components.icon.SvgIcon
import io.github.bric3.ij.ui.util.SvgImageExtension
import java.awt.Dimension
import java.io.InputStream
import javax.annotation.Priority
import javax.swing.JComponent
import javax.swing.JEditorPane
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.SwingConstants

class MiscTab : BorderLayoutPanel() {
    init {
        addToCenter(
            panel {
                customPopupComboBox()
                svgIconGroup()
                htmlPane()
                expandableSplitter()
            }
        )
    }

    private fun Panel.htmlPane() {
        group("Html Pane With SVG") {

            val srcProp = PropertyGraph().property(
                "https://github.com/bric3/fireplace/actions/workflows/build.yml/badge.svg"
            )
            row {
                textField()
                    .bindText(srcProp.trim())
                    .align(AlignX.FILL)
            }

            row {
                cell(
                    JEditorPane().apply {
                        contentType = "text/html"
                        editorKit = HTMLEditorKitBuilder()
                            .withViewFactoryExtensions(
                                SvgImageExtension(),
                                ExtendableHTMLViewFactory.Extensions.WORD_WRAP,
                            )
                            .withFontResolver(EditorCssFontResolver.getGlobalInstance())
                            .build()

                        UIUtil.doNotScrollToCaret(this)
                        caretPosition = 0
                        isEditable = false
                        foreground = JBColor.foreground()
                        isOpaque = false
                    }
                ).bindText(
                    srcProp.transform(
                        {
                            """
                            <html><h4>Shields.io SVG badge</h4>
                            <p><img src="$it" alt="Fireplace build badge" /></p>
                            </html>
                            """.trimIndent()
                        },
                        { "" }
                    )
                ).align(AlignX.FILL)
            }
        }
    }

    private fun Panel.customPopupComboBox() {
        group("Various") {
            row {
                actionLink(object : DumbAwareAction("Dialog Example") {
                    override fun actionPerformed(e: AnActionEvent) {
                        ExampleDialog(e.project!!).show()
                    }
                })

                label("Custom Popup ComboBox").align(AlignX.RIGHT)
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
                ).align(AlignX.RIGHT)
            }
        }
    }

    class MyPopupCreationContext(
        private val combo: ComboBoxWithCustomPopup<String>,
        private val values: Map<String, String>
    ) :
        ComboBoxWithCustomPopup.PopupCreationContext {
        private val detailsVisibility = AtomicLazyProperty { false }
        private val hoveredDetail = AtomicLazyProperty { "" }
        private val details = panel {
            group("Details:") {
                row {
                    label("").bindText(hoveredDetail)
                }
            }
            customize(UnscaledGaps(left = 3, right = 3))
        }.bindVisible(detailsVisibility)
            .withMinimumWidth(150)
            .withPreferredWidth(150)

        @Suppress("UnstableApiUsage")
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
                addToCenter(
                    BorderLayoutPanel()
                        .addToCenter(list)
                        .addToBottom(panel {
                            row {
                                actionButtonWithText(object : DumbAwareToggleAction("Show details") {
                                    override fun isSelected(e: AnActionEvent) = detailsVisibility.get()
                                    override fun setSelected(e: AnActionEvent, state: Boolean) =
                                        detailsVisibility.set(state)

                                    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
                                })
                            }
                        })
                )

                // Applying the border here, instead of content
                // to mimic other popup lists.
                border = JBUI.Borders.empty(PopupUtil.getListInsets(false, false))
                PopupUtil.applyNewUIBackground(this)

                detailsVisibility.afterChange {
                    combo.reshapePopup(
                        when (it) {
                            true -> Dimension(list.width + details.preferredWidth, height)
                            false -> Dimension(list.width, height)
                        }
                    )
                }
            }
        }

        private fun Row.actionButtonWithText(action: AnAction): Cell<ActionButtonWithText> {
            return cell(
                ActionButtonWithText(
                    action,
                    action.templatePresentation.clone(),
                    "dsl",
                    ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE
                )
            )
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
            row {
                icon(SvgIcon.fromStream(classpath("/icons/excalidraw-logo.svg"), 100))
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