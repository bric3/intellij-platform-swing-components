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
package io.github.bric3.ij.components.demo.toolwindow

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import io.github.bric3.ij.components.demo.toolwindow.misc.MiscTab
import io.github.bric3.ij.components.demo.toolwindow.tables.ScalableTablesTab
import io.github.bric3.ij.components.demo.toolwindow.tables.TableWithHoverToolbarTab

class DemoToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        println("createToolWindowContent")
        contents(ContentFactory.getInstance()).forEach {
            toolWindow.contentManager.addContent(it)
        }

        toolWindow.setTitleActions(listOf(
            DumbAwareAction.create("Refresh", AllIcons.Actions.Refresh) {
                toolWindow.contentManager.removeAllContents(true)

                contents(ContentFactory.getInstance()).forEach {
                    toolWindow.contentManager.addContent(it)
                }
            }
        ))
    }

    override fun shouldBeAvailable(project: Project) = true

    private fun contents(contentFactory: ContentFactory) = listOf(
        ScalableTablesTab.tabInfo,
        TableWithHoverToolbarTab.tabInfo,
        MiscTab.tabInfo,
    ).map {
        contentFactory.createContent(it.component, it.text, false)
    }
}