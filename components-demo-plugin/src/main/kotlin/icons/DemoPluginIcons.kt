package icons

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object DemoPluginIcons {
    @JvmStatic
    private fun load(path: String): Icon {
        return IconLoader.getIcon(path, DemoPluginIcons::class.java)
    }

    /** 20x20 */ @JvmField val PluginIcon = load("/icons/pluginIcon.svg")
}