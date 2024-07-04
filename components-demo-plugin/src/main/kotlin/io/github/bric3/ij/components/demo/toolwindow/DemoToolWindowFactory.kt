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
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.tabs.TabInfo
import io.github.classgraph.ClassGraph
import javax.annotation.Priority


internal class DemoToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        toolWindow.setAnchor(ToolWindowAnchor.RIGHT, null)
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

    private fun contents(contentFactory: ContentFactory) = tabFactories().map {
        val tabInfo = it.createTab()

        contentFactory.createContent(tabInfo.component, tabInfo.text, false).apply {
            it.customizeContent(this)
        }
    }

    private fun tabFactories(): Sequence<TabFactory> {
        return ClassGraph()
            .enableAllInfo()
            .acceptPackages(javaClass.packageName)
            .scan()
            .use { scanResult ->
                scanResult.getClassesImplementing(TabFactory::class.java.name)
                    .asSequence()
                    .map { it.loadClass(TabFactory::class.java) }
                    .sortedBy { it.getAnnotation(Priority::class.java)?.value ?: 1000 }
                    .map { it.getDeclaredConstructor().newInstance() }
                    .toList()
            }
            .asSequence()
    }

    interface TabFactory {
        fun createTab(): TabInfo
        fun customizeContent(content: Content) {}
    }
}