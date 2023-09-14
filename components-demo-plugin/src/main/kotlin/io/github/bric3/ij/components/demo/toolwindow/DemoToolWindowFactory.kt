@file:Suppress("DialogTitleCapitalization")

package io.github.bric3.ij.components.demo.toolwindow

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.tabs.impl.SingleHeightTabs
import io.github.bric3.ij.components.demo.toolwindow.tables.ScalableTables

class DemoToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val demoToolWindow = DemoToolWindow(project, toolWindow)
        val content = ContentFactory.getInstance().createContent(
            demoToolWindow.getContent(),
            "Swing Components",
            false
        )
        toolWindow.contentManager.addContent(content)
        toolWindow.setTitleActions(listOf(
            DumbAwareAction.create("Refresh", AllIcons.Actions.Refresh) {
                toolWindow.contentManager.removeAllContents(true)

                val newContent = ContentFactory.getInstance().createContent(
                    demoToolWindow.getContent(),
                    "Swing Components",
                    false
                )
                toolWindow.contentManager.addContent(newContent)
            }
        ))
    }

    override fun shouldBeAvailable(project: Project) = true

    class DemoToolWindow(private val project: Project, private val toolWindow: ToolWindow) {
        fun getContent() = SingleHeightTabs(project, toolWindow.disposable).apply {
            addTab(ScalableTables.tabInfo)
        }
    }
}