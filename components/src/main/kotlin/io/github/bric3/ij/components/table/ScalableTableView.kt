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
package io.github.bric3.ij.components.table

import com.intellij.ui.table.JBTable
import com.intellij.ui.table.TableView
import com.intellij.util.ui.ListTableModel
import java.lang.invoke.MethodHandles

/**
 * Scalable [TableView].
 *
 * The Jetbrains [TableView] does not scale correctly custom renderer
 * to presentation mode, this patched class fixes the issue.
 *
 * See [IDEA-289745](https://youtrack.jetbrains.com/issue/IDEA-289745).
 *
 * @see ScalableJBTable
 */
class ScalableTableView<Item> : TableView<Item> {
    @Suppress("PrivatePropertyName")
    private val myRowHeight_VH = try {
        MethodHandles.privateLookupIn(
            JBTable::class.java,
            MethodHandles.lookup()
        ).findVarHandle(JBTable::class.java, "myRowHeight", Int::class.javaPrimitiveType)
    } catch (e: NoSuchFieldException) {
        throw IllegalStateException(
            "Fix to recalculate row height on updateUI (like switching to/from presentation mode) broken, please update",
            e
        )
    }

    constructor() : super()

    constructor(model: ListTableModel<Item>?) : super(model)

    override fun updateUI() {
        // JBTable.getRowHeight has a custom implementation
        // that caches the row height. Unfortunately, once the value
        // is computed, it is not updated upon updateUI, like when switching
        // to/from presentation mode.
        // Unfortunately, there's no API that allows to reset the calculation
        // This fix relies on the fact that when the JBTable.myRowHeight is
        // inferior to zero, the calculation of the row height happens again.
        //
        // see https://youtrack.jetbrains.com/issue/IDEA-289745
        //
        // updateUI can be called by parent before the field is initialized
        myRowHeight_VH?.set(this, -1)
        super.updateUI()
    }
}