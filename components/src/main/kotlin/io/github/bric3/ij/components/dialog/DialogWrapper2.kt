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
package io.github.bric3.ij.components.dialog

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.DialogWrapperPeer
import com.intellij.openapi.ui.OptionAction
import org.jetbrains.annotations.Nls
import java.awt.Component
import java.awt.event.ActionEvent
import java.awt.event.InputEvent
import java.util.function.Function
import javax.swing.AbstractAction
import javax.swing.Action

/**
 * Same as regular [DialogWrapper] but provides factory methods to convert
 * IJ's [AnAction] to Swing's [Action] and [OptionAction].
 *
 * @see [swingAction]
 * @see [swingChoiceAction]
 */
@Suppress("unused")
abstract class DialogWrapper2 : DialogWrapper {
    constructor(project: Project?, canBeParent: Boolean) : super(project, canBeParent)
    constructor(project: Project?, canBeParent: Boolean, ideModalityType: IdeModalityType) : super(project, canBeParent, ideModalityType)
    constructor(project: Project?, parentComponent: Component?, canBeParent: Boolean, ideModalityType: IdeModalityType) : super(project, parentComponent, canBeParent, ideModalityType)
    constructor(project: Project?, parentComponent: Component?, canBeParent: Boolean, ideModalityType: IdeModalityType, createSouth: Boolean) : super(project, parentComponent, canBeParent, ideModalityType, createSouth)
    constructor(project: Project?) : super(project)
    constructor(canBeParent: Boolean) : super(canBeParent)
    constructor(canBeParent: Boolean, applicationModalIfPossible: Boolean) : super(canBeParent, applicationModalIfPossible)
    constructor(project: Project?, canBeParent: Boolean, applicationModalIfPossible: Boolean) : super(project, canBeParent, applicationModalIfPossible)
    constructor(parent: Component, canBeParent: Boolean) : super(parent, canBeParent)
    constructor(peerFactory: Function<in DialogWrapper, out DialogWrapperPeer>) : super(peerFactory)


    /**
     * Make this action the pre-focused one.
     * Does not work in sub-choices of an [OptionAction].
     *
     * ```kotlin
     * buildList {
     *   add(swingAction(
     *     DumbAwareAction.create("The pre focused button") {
     *       ...perform something
     *     }
     *   ).makeDefault())
     * }
     * ```
     */
    protected fun Action.makeDefault(): Action = apply {
        putValue(DEFAULT_ACTION, true)
    }

    /**
     * Convert an IJ [AnAction] to a Swing [Action].
     *
     * Example usage (that can be returned from [createActions]):
     *
     * ```kotlin
     * buildList {
     *    add(swingAction(
     *        DumbAwareAction.create("Refresh") {
     *          ...refresh something in dialog
     *        }
     *    ))
     *    add(swingAction(
     *        DumbAwareAction.create("Activate") {
     *          ...activate something
     *        }
     *    ), {
     *        close(NEXT_USER_EXIT_CODE + 2) // close the dialog with custom exit code
     *    })
     * ```
     *
     * @param anAction the IJ action to convert
     * @param andThen a callback to execute after the action has been performed, default to closing the dialog
     *                with [DialogWrapper.OK_EXIT_CODE]
     * @see swingChoiceAction
     */
    protected fun swingAction(anAction: AnAction, andThen: () -> Unit = { close(OK_EXIT_CODE) }): Action {
        return object : DialogWrapperAction(
                anAction.templatePresentation.textWithMnemonic
                        ?: anAction.templatePresentation.text
        ) {
            init {
                putValue(AN_ACTION_KEY, anAction)
            }

            override fun doAction(e: ActionEvent) {
                val dataContext = DataManager.getInstance().getDataContext(
                        this@DialogWrapper2.contentPanel
                )
                val inputEvent = if (e.source is InputEvent) e.source as InputEvent else null
                val event = AnActionEvent.createFromAnAction(anAction, inputEvent, ActionPlaces.UNKNOWN, dataContext)
                ActionUtil.performActionDumbAwareWithCallbacks(anAction, event)
                andThen()
            }
        }
    }

    /**
     * Convert several IJ [AnAction] to a multiple choice Swing [Action].
     *
     * It looks like the git push dialog, with multiple choice for pushing (Push, Push Force).
     *
     * Example usage (that can be returned from [createActions]):
     * ```kotlin
     * buildList {
     *   add(swingChoiceAction(
     *         swingAction(ActionUtil.getAction(my.main.actionId)),
     *         swingAction(actionAlternative1),
     *         swingAction(actionAlternative2),
     *   )
     * }
     * ```
     *
     * @param defaultAction the default action to perform when the user clicks on the button
     * @param options the options to display in the dropdown
     * @see swingAction
     */
    protected fun swingChoiceAction(defaultAction: Action, vararg options: Action): OptionAction {
        return object : AbstractAction(defaultAction.name), OptionAction {
            override fun actionPerformed(e: ActionEvent) {
                defaultAction.actionPerformed(e)
            }

            override fun setEnabled(isEnabled: Boolean) {
                super.setEnabled(isEnabled)
                for (optionAction in options) {
                    optionAction.isEnabled = isEnabled
                }
            }

            override fun getOptions(): Array<out Action> {
                return options
            }
        }
    }

    companion object {
        private const val AN_ACTION_KEY = "dd.anAction"
    }
}

var Action.name: @Nls String?
    get() = getValue(Action.NAME) as? String
    set(value) {
        putValue(Action.NAME, value)
    }

fun Action.toAnAction(): AnAction {
    val action = this
    return object : DumbAwareAction(action.name.orEmpty()) {
        override fun getActionUpdateThread(): ActionUpdateThread {
            return ActionUpdateThread.EDT
        }

        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = action.isEnabled
        }

        override fun actionPerformed(event: AnActionEvent) {
            val actionEvent = ActionEvent(event.getData(PlatformCoreDataKeys.CONTEXT_COMPONENT),
                ActionEvent.ACTION_PERFORMED,
                "execute",
                event.modifiers)
            action.actionPerformed(actionEvent)
        }
    }
}
