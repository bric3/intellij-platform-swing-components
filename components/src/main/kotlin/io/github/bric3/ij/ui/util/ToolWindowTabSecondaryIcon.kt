package io.github.bric3.ij.ui.util

import com.intellij.openapi.application.EDT
import com.intellij.openapi.util.Key
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.impl.content.ToolWindowContentUi
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentManagerEvent
import com.intellij.ui.content.ContentManagerListener
import com.intellij.ui.content.impl.ContentImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.awt.ComponentOrientation
import javax.swing.Icon
import kotlin.reflect.full.declaredMembers
import kotlin.time.Duration.Companion.milliseconds

object ToolWindowTabSecondaryIcon {
    fun install(content: Content, icon: Icon, iconInstaller: (Content, Icon) -> Unit = simpleIconInstaller) {
        with(content) {
            putUserData(ToolWindow.SHOW_CONTENT_ICON, true)
            putUserData(Content.TAB_LABEL_ORIENTATION_KEY, ComponentOrientation.RIGHT_TO_LEFT)
            NOT_SELECTED_TAB_ICON_TRANSPARENT?.let { putUserData(it, false) }

            iconInstaller(this, icon)
        }
    }

    private val simpleIconInstaller: (Content, Icon) -> Unit = { content, icon ->
        content.icon = icon
    }

    fun removeOnFirstClickIconInstaller(scope: CoroutineScope): (Content, Icon) -> Unit = { content, icon ->
        tryInstallSelectionChanged(content)
        content.addPropertyChangeListener { event ->
            if (ContentImpl.PROP_CONTENT_MANAGER == event.propertyName) {
                tryInstallSelectionChanged(content)
            }
        }
        val wasEverOpenedStateFlow = WasEverOpenedService.getInstance().wasEverOpenedStateFlow
        if (!wasEverOpenedStateFlow.value) {
            content.icon = icon

            scope.launch(Dispatchers.EDT) {
                wasEverOpenedStateFlow.filter { it }.first() // stop after first true value
                delay(300.milliseconds)

                content.icon = null
            }
        }
    }

    private fun tryInstallSelectionChanged(content: Content) {
        content.manager?.addContentManagerListener(object : ContentManagerListener {
            override fun selectionChanged(event: ContentManagerEvent) {
                val selected = ContentManagerEvent.ContentOperation.add == event.operation
                if (event.content == content) {
                    if (selected) {
                        WasEverOpenedService.getInstance().setWasOpened(true)
                    }
                }
            }
        })
    }

    // See https://youtrack.jetbrains.com/issue/IJPL-157380/ToolWindowContentUi.NOTSELECTEDTABICONTRANSPARENT-is-internal
    private val NOT_SELECTED_TAB_ICON_TRANSPARENT: Key<Boolean>? = ToolWindowContentUi::class
        .declaredMembers
        .firstOrNull { it.name == "NOT_SELECTED_TAB_ICON_TRANSPARENT"}
        ?.call() as? Key<Boolean>
}