@file:Suppress("DialogTitleCapitalization")

package io.github.bric3.ij.components.demo.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.tabs.impl.SingleHeightTabs

class DemoToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val demoToolWindow = DemoToolWindow(project, toolWindow)
        val content = ContentFactory.getInstance().createContent(
            demoToolWindow.getContent(),
            "Swing Components",
            false
        )
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true

    class DemoToolWindow(private val project: Project, private val toolWindow: ToolWindow) {
        fun getContent() = SingleHeightTabs(project, toolWindow.disposable).apply {
            addTab(Tab1.tabInfo)
        }
    }
}