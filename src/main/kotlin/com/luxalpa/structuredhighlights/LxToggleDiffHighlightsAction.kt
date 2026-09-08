package com.luxalpa.structuredhighlights

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.project.DumbAware

/**
 * Toggles whether structured highlights are suppressed inside diff viewers.
 *
 * Shown as "on" when highlights are visible in diffs, i.e. it is the inverse of
 * [LxApplicationSettings.disableInDiff].
 */
class LxToggleDiffHighlightsAction : ToggleAction(
    MyBundle.messagePointer("action.toggleDiffHighlights.text")
), DumbAware {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun isSelected(e: AnActionEvent): Boolean =
        !LxApplicationSettings.instance.disableInDiff

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        LxApplicationSettings.instance.disableInDiff = !state
        restartHighlighting()
    }
}
