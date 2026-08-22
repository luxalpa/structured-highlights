package com.luxalpa.structuredhighlights

import com.intellij.openapi.editor.colors.ColorKey
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.psi.PsiFile
import java.awt.Color

interface LanguageSupport {
    companion object {
        val EP = ExtensionPointName.create<LanguageSupport>(
            "com.luxalpa.structuredhighlights.languageSupport"
        )
    }

    // The descriptors returned here must be in the correct order! The ones that should draw in the background need to
    // come before the ones that should render on top of them.
    fun collectDescriptors(file: PsiFile): List<DefinitionBlockDescriptor>?
    val blockTypes: List<BlockType>
    val displayName: String
}

interface BlockType {
    val label: String
    val key: String
    val defaultColor: Color
    val colorKey: ColorKey
}