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