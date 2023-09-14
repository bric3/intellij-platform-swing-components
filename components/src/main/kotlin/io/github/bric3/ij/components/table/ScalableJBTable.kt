package io.github.bric3.ij.components.table

import com.intellij.ui.table.JBTable
import java.lang.invoke.MethodHandles
import javax.swing.table.TableColumnModel
import javax.swing.table.TableModel

/**
 * Scalable [JBTable].
 *
 * The Jetbrains [JBTable] does not scale correctly custom renderer
 * to presentation mode, this patched class fixes the issue.
 *
 * See [IDEA-289745](https://youtrack.jetbrains.com/issue/IDEA-289745).
 *
 * @see ScalableTableView
 */
class ScalableJBTable : JBTable {
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
    constructor(model: TableModel?) : super(model)
    constructor(model: TableModel?, columnModel: TableColumnModel?) : super(model, columnModel)

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