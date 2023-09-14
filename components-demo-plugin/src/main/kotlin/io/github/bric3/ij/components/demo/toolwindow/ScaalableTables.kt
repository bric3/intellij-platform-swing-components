@file:Suppress("DialogTitleCapitalization")

package io.github.bric3.ij.components.demo.toolwindow

import com.intellij.openapi.actionSystem.ActionToolbarPosition
import com.intellij.ui.ColorUtil
import com.intellij.ui.HtmlToSimpleColoredComponentConverter
import com.intellij.ui.JBColor
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.TableSpeedSearch
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.table.JBTable
import com.intellij.ui.table.TableView
import com.intellij.ui.tabs.TabInfo
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.ListTableModel
import com.intellij.util.ui.components.BorderLayoutPanel
import io.github.bric3.ij.components.table.ScalableJBTable
import io.github.bric3.ij.components.table.ScalableTableView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import java.util.Comparator.comparing
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JProgressBar
import javax.swing.JTable
import javax.swing.SwingConstants
import javax.swing.table.TableCellRenderer
import kotlin.time.Duration.Companion.seconds

typealias NumberToJapanese = Triple<Int, String, String>

class ScaalableTables : BorderLayoutPanel() {
    private val cs = CoroutineScope(SupervisorJob())

    init {
        addToCenter(
            panel {
                row {
                    comment("Try zooming, entering/exiting presentation mode, etc.")
                }
                group(ScalableJBTable::class.simpleName!!, false) {
                    row {
                        scrollCell(table(::ScalableJBTable)).align(Align.FILL)
                            .comment("This table is a JBTable that scales correctly the custom renderer when to presentation mode")
                    }.resizableRow()
                }.resizableRow()
                group(ScalableTableView::class.simpleName!!, false) {
                    row {
                        scrollCell(table { ScalableTableView(it) }).align(Align.FILL)
                            .comment("This table is a JBTable that scales correctly the custom renderer when to presentation mode")
                    }.resizableRow()
                }.resizableRow()

                border = JBUI.Borders.empty(5)
            }
        )
    }

    private fun table(tableConstructor: (ListTableModel<*>) -> JBTable): JPanel {
        val model = modelLoadingAsynchronously()

        return tableConstructor(model).run {
            TableSpeedSearch.installOn(this)

            // If not a subtype of TableView (which is a JBTable), then we need to set the renderer
            if (this !is TableView<*>) {
                model.columnInfos.forEachIndexed { index, columnInfo ->
                    // ignoring item, as it's always the same renderer
                    columnModel.getColumn(index).cellRenderer = columnInfo.getRenderer(null)
                }
            }

            ToolbarDecorator.createDecorator(this).also {
                it.setToolbarPosition(ActionToolbarPosition.LEFT)
                it.setPanelBorder(JBUI.Borders.customLineTop(JBColor.border()))
                it.setScrollPaneBorder(JBUI.Borders.empty())

                it.disableUpDownActions()

                it.setAddActionName("Add")
                it.setAddAction {
                    // todo
                }

                it.setRemoveActionName("Remove")
                it.setRemoveAction {
                    // todo
                }
                it.setRemoveActionUpdater {
                    this.selectedRowCount > 0
                }
            }.createPanel()
        }
    }

    private fun modelLoadingAsynchronously(): ListTableModel<NumberToJapanese> {
        val model = ListTableModel<NumberToJapanese>(
            PairOfStringColumnInfo("English to Japanese")
        )

        cs.launch {
            while (true) {
                model.addRow(Triple(0, "Zero", "Zero"))
                delay(1.seconds)
                model.addRow(Triple(1, "One", "Ichi"))
                delay(1.seconds)
                model.addRow(Triple(2, "Two", "Ni"))
                delay(1.seconds)
                model.addRow(Triple(3, "Three", "San"))
                delay(1.seconds)
                model.addRow(Triple(4, "Four", "Shi"))
                delay(1.seconds)
                model.addRow(Triple(5, "Five", "Go"))
                delay(1.seconds)
                model.addRow(Triple(6, "Six", "Roku"))
                delay(1.seconds)
                model.addRow(Triple(7, "Seven", "Shichi"))
                delay(1.seconds)
                model.addRow(Triple(8, "Eight", "Hachi"))
                delay(1.seconds)
                model.addRow(Triple(9, "Nine", "Kyu"))
                delay(1.seconds)
                model.addRow(Triple(10, "Ten", "Ju"))
                delay(1.seconds)
                model.addRow(Triple(11, "Eleven", "Juichi"))
                delay(1.seconds)
                model.addRow(Triple(12, "Twelve", "Junii"))
                delay(1.seconds)
                model.addRow(Triple(13, "Thirteen", "Jusan"))
                delay(1.seconds)
                model.addRow(Triple(14, "Fourteen", "Jushi"))
                delay(1.seconds)
                model.addRow(Triple(15, "Fifteen", "Jugo"))
                delay(1.seconds)
                model.addRow(Triple(16, "Sixteen", "Juroku"))
                delay(30.seconds)
                model.items = mutableListOf()
            }
        }
        return model
    }


    class PairOfStringColumnInfo(name: String) : ColumnInfo<NumberToJapanese, NumberToJapanese>(name) {
        override fun valueOf(item: NumberToJapanese?): NumberToJapanese? {
            return item
        }

        override fun getRenderer(item: NumberToJapanese?): TableCellRenderer {
            return ProgressRenderer()
        }

        override fun getComparator(): Comparator<NumberToJapanese>? {
            return comparing(NumberToJapanese::first)
                .thenComparing(
                    comparing(
                        NumberToJapanese::second,
                        java.lang.String.CASE_INSENSITIVE_ORDER
                    )
                ).thenComparing(
                    comparing(
                        NumberToJapanese::third,
                        java.lang.String.CASE_INSENSITIVE_ORDER
                    )
                )
        }
    }

    class ProgressRenderer : BorderLayoutPanel(), TableCellRenderer {
        private val htmlConverter = HtmlToSimpleColoredComponentConverter()

        private val simpleColoredComponent = SimpleColoredComponent().apply {
            border = JBUI.Borders.empty(4)
        }

        init {
            isOpaque = true
            border = JBUI.Borders.customLine(ColorUtil.withAlpha(JBColor.RED, 0.5), 1)
            add(simpleColoredComponent, BorderLayout.CENTER)
            add(JProgressBar(SwingConstants.HORIZONTAL, 0, 100).apply { value = 50 }, BorderLayout.SOUTH)
        }

        @Suppress("UNCHECKED_CAST")
        override fun getTableCellRendererComponent(
            table: JTable?,
            value: Any?,
            isSelected: Boolean,
            hasFocus: Boolean,
            row: Int,
            column: Int,
        ): JComponent? {
            val (a, b, c) = value as? NumberToJapanese ?: return null
            simpleColoredComponent.apply {
                clear()
                setTextAlign(SwingConstants.LEFT)

                htmlConverter.appendHtml(
                    this,
                    "number: $a: ",
                    SimpleTextAttributes.GRAYED_BOLD_ATTRIBUTES
                )

                append(
                    b,
                    SimpleTextAttributes.ERROR_ATTRIBUTES.derive(SimpleTextAttributes.STYLE_WAVED, null, null, null)
                )
                append(" - ")
                append(c, SimpleTextAttributes.REGULAR_ITALIC_ATTRIBUTES)
            }

            return this
        }
    }


    companion object {
        val tabInfo
            get() = TabInfo(ScaalableTables()).apply {
                setText("Scalable Tables")
            }
    }
}