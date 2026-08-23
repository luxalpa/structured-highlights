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
    
    fun collectDescriptors(file: PsiFile): List<DefinitionBlockDescriptor>?
    val blockTypes: List<BlockType>
    val displayName: String
    val languageId: String
    val previewText: String
}

interface BlockType {
    val label: String
    val key: String
    val defaultColor: Color
    val colorKey: ColorKey
}