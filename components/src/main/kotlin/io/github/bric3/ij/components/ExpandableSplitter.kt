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
package io.github.bric3.ij.components

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.observable.properties.AtomicBooleanProperty
import com.intellij.openapi.observable.properties.ObservableMutableProperty
import com.intellij.openapi.observable.util.bindVisible
import com.intellij.openapi.util.NlsActions
import com.intellij.ui.OnePixelSplitter
import com.intellij.util.ui.components.BorderLayoutPanel
import org.jetbrains.annotations.Nls
import java.util.function.Consumer
import java.util.function.Supplier
import javax.swing.JComponent
import javax.swing.SwingConstants.LEFT
import javax.swing.SwingConstants.RIGHT

/**
 * Creates a split panel where one side can be expanded or collapsed.
 *
 * The [expandableComponent] has the opportunity to get the collapse action
 * to be placed in a toolbar, the best location being a vertical toolbar on
 * the LEFT or RIGHT side.
 *
 * Example:
 * ```kotlin
 * ExpandableSplitter(
 *     "ExpandableSplitter",
 *     SwingConstants.LEFT,
 * ) {
 *     val expandableToolbar = ActionManager.getInstance().createActionToolbar(
 *         "my.expandable",
 *         DefaultActionGroup(
 *             getCollapseAction(),
 *             ...
 *         ),
 *         false
 *     )
 *     expandableComponent = BorderLayoutPanel()
 *         .addToLeft(expandableToolbar.component)
 *         .addToCenter(/* Expandable JComponent */)
 *     expandableToolbar.targetComponent = expandableComponent
 *     mainComponent = /* Main JComponent */
 * }
 * ```
 *
 * Current limitation the splitter only supports horizontal orientation,
 * that is the [expandableSide] can only be on either the [right][RIGHT] or [left][LEFT].
 *
 * @param expandableText the text to display on the expandable button
 * @param expandableSide the side on which the expandable component is displayed
 * @param initialRatio the initial ratio of the splitter
 * @param configurer the configurer allows configuring the whole component in a contextual way
 *
 * @see VerticalExpandButton
 */
class ExpandableSplitter(
    @Nls(capitalization = Nls.Capitalization.Title) expandableText: String,
    @VerticalExpandButton.SidePlacement private val expandableSide: Int = LEFT,
    initialRatio: Float = computeDefaultRatio(expandableSide),
    configurer: Configurer.() -> Unit
) : BorderLayoutPanel() {
    private val isExpandedProperty: ObservableMutableProperty<Boolean> = AtomicBooleanProperty(false)

    private val verticalButton = VerticalExpandButton(
        expandableText,
        expandableSide,
        isExpandedProperty,
    )
    // possible alternative use a Card Layout
    private val splitter = OnePixelSplitter(
        false,
        initialRatio,
        0.2f,
        0.8f
    )

    /**
     * This constructor is tailored for Java usage.
     *
     * @see ExpandableSplitter
     */
    @JvmOverloads
    constructor(
        @Nls(capitalization = Nls.Capitalization.Title) expandableText: String,
        @VerticalExpandButton.SidePlacement expandableSide: Int = LEFT,
        initialRatio: Float = computeDefaultRatio(expandableSide),
        configurer: Consumer<Configurer>
    ) : this(expandableText, expandableSide, initialRatio, configurer::accept)

    init {
        // possible improvement: allow vertical orientation
        require(expandableSide == LEFT || expandableSide == RIGHT) { "Use either SwingConstants.LEFT or SwingConstants.RIGHT" }
        configurer.invoke(Configurer()).also {
            require(splitter.firstComponent != null || splitter.secondComponent == null) { "main or expandable component are not set" }
        }

        addToCenter(splitter)
        when (expandableSide) {
            LEFT -> addToLeft(verticalButton)
            RIGHT -> addToRight(verticalButton)
        }
    }

    /**
     * Whether the expandable component is expanded or collapsed.
     *
     * Read or propagate the value from or to [isExpandedProperty].
     */
    var isExpanded: Boolean
        set(value) { isExpandedProperty.set(value) }
        get() = isExpandedProperty.get()

    /**
     * The main component of the splitter.
     *
     * This is the component that will always be displayed.
     */
    var mainComponent: JComponent
        set(main) {
            when (expandableSide) {
                LEFT -> splitter.secondComponent = main
                RIGHT -> splitter.firstComponent = main
            }
        }
        get() = when (expandableSide) {
            LEFT -> splitter.secondComponent
            RIGHT -> splitter.firstComponent
            else -> error("expandableSide is neither LEFT or RIGHT")
        } as JComponent

    /**
     * The expandable component of the splitter.
     *
     * This is the component that will be displayed when expanded.
     */
    var expandableComponent: JComponent
        set(expandable) {
            when (expandableSide) {
                LEFT -> splitter.firstComponent = expandable
                RIGHT -> splitter.secondComponent = expandable
            }
            expandable.bindVisible(isExpandedProperty)
        }
        get() = when (expandableSide) {
            LEFT -> splitter.firstComponent
            RIGHT -> splitter.secondComponent
            else -> error("expandableSide is neither LEFT or RIGHT")
        } as JComponent

    inner class Configurer {
        @JvmOverloads
        fun getCollapseAction(
            dynamicText: Supplier<@NlsActions.ActionText String> = Supplier { "Hide" },
            onCollapseAction: Runnable? = null
        ): AnAction = verticalButton.createCollapseAction(
            dynamicText,
            onCollapseAction
        )

        var mainComponent: JComponent by this@ExpandableSplitter::mainComponent
        var expandableComponent: JComponent by this@ExpandableSplitter::expandableComponent
    }

    companion object {
        private fun computeDefaultRatio(expandableComponentSide: Int): Float {
            return when (expandableComponentSide) {
                LEFT -> 0.33f
                RIGHT -> 0.66f
                else -> throw IllegalArgumentException("Use either SwingConstants.LEFT or SwingConstants.RIGHT")
            }
        }
    }
}