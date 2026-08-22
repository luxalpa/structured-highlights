package com.luxalpa.structuredhighlights

import com.intellij.application.options.colors.ColorAndFontOptions
import com.intellij.lang.Language
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.LanguageTextField
import com.intellij.util.ui.JBUI

class LxLanguageTextField(
    project: Project,
    text: String,
    languageId: String,
    val options: ColorAndFontOptions? = null,
) :
    LanguageTextField(Language.findLanguageByID(languageId)!!, project, text, false) {

    init {
        this.font = EditorFontType.PLAIN.globalFont
    }

    override fun createEditor(): EditorEx {
        val editor = super.createEditor()

        options?.selectedScheme?.let { scheme ->
            editor.colorsScheme = scheme
        }
        editor.setHorizontalScrollbarVisible(true)
        editor.setVerticalScrollbarVisible(true)
        editor.settings.isLineNumbersShown = true
        editor.settings.isAutoCodeFoldingEnabled = true
        editor.settings.isFoldingOutlineShown = true
        editor.settings.isIndentGuidesShown = true
        editor.settings.isLineMarkerAreaShown = true
        editor.settings.isCaretRowShown = true
        editor.isOneLineMode = false
        editor.setBorder(JBUI.Borders.customLine(JBColor.border(), 1, 1, 1, 1))
        editor.putUserData(LUX_PREVIEW_SETTINGS, LxApplicationSettings.instance.previewSettings)
        return editor
    }
}