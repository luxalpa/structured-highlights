package com.luxalpa.structuredhighlights

import com.intellij.openapi.editor.colors.ColorKey
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.psi.PsiFile
import com.intellij.psi.util.endOffset
import com.intellij.psi.util.startOffset
import java.awt.Color

interface LanguageSupport {
    companion object {
        val EP = ExtensionPointName.create<LanguageSupport>(
            "com.luxalpa.structuredhighlights.languageSupport"
        )
    }

    val languageId: String
    val displayName: String
    val previewText: String
    val blockTypes: List<BlockType>
    fun collectDescriptors(file: PsiFile): List<DefinitionBlockDescriptor>?
}

interface BlockType {
    val label: String
    val key: String
    val defaultColor: Color
    val colorKey: ColorKey
}