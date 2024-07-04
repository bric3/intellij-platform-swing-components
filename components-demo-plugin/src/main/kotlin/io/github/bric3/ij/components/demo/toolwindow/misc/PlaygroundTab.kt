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

import com.intellij.ide.IdeTooltip
import com.intellij.ide.IdeTooltipManager
import com.intellij.lang.documentation.DocumentationSettings
import com.intellij.openapi.editor.impl.EditorCssFontResolver
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.ColorUtil
import com.intellij.ui.JBColor
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.content.Content
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.tabs.TabInfo
import com.intellij.util.ui.ExtendableHTMLViewFactory
import com.intellij.util.ui.HTMLEditorKitBuilder
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBInsets
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import io.github.bric3.ij.components.demo.toolwindow.DemoToolWindowFactory
import io.github.bric3.ij.components.icon.TextIcon
import io.github.bric3.ij.ui.util.ToolWindowTabSecondaryIcon
import io.github.bric3.ij.ui.util.WasEverOpenedService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.awt.Color
import javax.annotation.Priority
import javax.swing.JComponent
import javax.swing.JEditorPane
import javax.swing.JLabel
import javax.swing.event.HyperlinkEvent
import javax.swing.event.HyperlinkListener
import javax.swing.text.html.HTMLDocument

class PlaygroundTab: BorderLayoutPanel() {
    init {
        addToCenter(
            panel {
                row {
                    cell(
                        JEditorPane().apply {
                            addHyperlinkListener(
                                object : HyperlinkListener {
                                    override fun hyperlinkUpdate(e: HyperlinkEvent) {
                                        val editorPane = e.inputEvent.component as? JEditorPane ?: return
                                        val styledDocument = editorPane.document as? HTMLDocument ?: return
                                        e.sourceElement ?: return

                                        val characterElement = styledDocument.getCharacterElement(e.sourceElement.startOffset)

                                        val style = when (e.eventType) {
                                            HyperlinkEvent.EventType.ENTERED -> styledDocument.getStyle("a:hover")
                                            HyperlinkEvent.EventType.EXITED -> styledDocument.getStyle(".highlight")
                                            else -> return
                                        }

                                        styledDocument.setCharacterAttributes(
                                            characterElement.startOffset,
                                            characterElement.endOffset - characterElement.startOffset,
                                            style,
                                            false
                                        )
                                    }
                                }
                            )

                            contentType = "text/html"

                            editorKit = HTMLEditorKitBuilder()
                                .withViewFactoryExtensions(ExtendableHTMLViewFactory.Extensions.WORD_WRAP)
                                .withFontResolver(EditorCssFontResolver.getGlobalInstance())
                                .build()
                                .also {
                                    @Suppress("UnstableApiUsage")
                                    val editorFontStyle =
                                        """{ font-family:"${EditorCssFontResolver.EDITOR_FONT_NAME_NO_LIGATURES_PLACEHOLDER}";font-size:${
                                            DocumentationSettings.getMonospaceFontSizeCorrection(false)
                                        }%; }"""

                                    it.styleSheet.apply {
                                        addRule("code $editorFontStyle")
                                        // addRule("a { color: ${ColorUtil.toHtmlColor(JBUI.CurrentTheme.Link.Foreground.ENABLED)}; text-decoration: none;}")
                                    }
                                }

                            text = """
                                <html><body>
                                <code>some code and <a class="highlight" href="highlight-1">some highlighted stuff</a></code>
                                </body></html>
                                """.trimIndent()

                            (document as HTMLDocument).apply {
                                styleSheet.addRule(
                                    """
                                    a { text-decoration: none; }
                                    .highlight {
                                        color: ${ColorUtil.toHtmlColor(foreground)};
                                        background-color: ${ColorUtil.toHtmlColor(JBColor.YELLOW)};
                                        text-decoration: none;
                                    }
                                    a:hover {
                                        color: ${ColorUtil.toHtmlColor(foreground)};
                                        background-color: ${ColorUtil.toHtmlColor(JBColor.ORANGE)};
                                        text-decoration: none;
                                    }""".trimIndent()
                                )
                            }

                            UIUtil.doNotScrollToCaret(this)
                            caretPosition = 0
                            isEditable = false
                        }
                    )
                }
            }
        )
    }

    @Priority(4)
    class Factory : DemoToolWindowFactory.TabFactory {
        override fun createTab() = TabInfo(PlaygroundTab()).apply {
            setText("Playground")
        }

        override fun customizeContent(content: Content) {
            // reset for demo purpose
            WasEverOpenedService.getInstance().loadState(WasEverOpenedService.State())

            val icon = TextIcon(
                text = "Beta",
                font = JBFont.medium().asBold(),
                foregroundColor = Color(0x007444D4),
                backgroundColor = Color(0x00DCCBFB),
                arcw = 8.0,
                arch = 8.0,
            )
            ToolWindowTabSecondaryIcon.install(
                content = content,
                icon = icon,
                iconInstaller = ToolWindowTabSecondaryIcon.removeOnFirstClickIconInstaller(CoroutineScope(Dispatchers.Main))
            )
        }
    }
}