package com.luxalpa.structuredhighlights

import com.intellij.psi.PsiFile
import com.jetbrains.rider.cpp.fileType.psi.CppFile
import com.luxalpa.structuredhighlights.shared.BlockType
import com.luxalpa.structuredhighlights.shared.DefinitionBlockDescriptor
import com.luxalpa.structuredhighlights.shared.LanguageSupport

class CppLangSupp : LanguageSupport {
    val file: CppFile? = null
    override val languageId: String = "C++"
    override val displayName: String = "C/C++"
    override val previewText: String = "some C"
    override val blockTypes: List<BlockType> = listOf()

    override fun collectDescriptors(file: PsiFile): List<DefinitionBlockDescriptor>? {
        return null
    }
}