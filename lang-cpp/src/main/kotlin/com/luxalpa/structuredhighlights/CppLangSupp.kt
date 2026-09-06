package com.luxalpa.structuredhighlights

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.openapi.editor.colors.ColorKey
import com.intellij.psi.PsiFile
import com.jetbrains.rider.cpp.fileType.psi.CppFile
import com.luxalpa.structuredhighlights.shared.BlockCollector
import com.luxalpa.structuredhighlights.shared.BlockType
import com.luxalpa.structuredhighlights.shared.DefaultColor
import com.luxalpa.structuredhighlights.shared.DefinitionBlockDescriptor
import com.luxalpa.structuredhighlights.shared.LanguageSupport
import com.luxalpa.structuredhighlights.shared.debug
import org.intellij.lang.annotations.Language
import java.awt.Color

class CppLangSupp : LanguageSupport {
    val file: CppFile? = null
    override val languageId: String = "C++"
    override val displayName: String = "C/C++"
    override val previewText: String = PREVIEW_TEXT
    override val blockTypes: List<BlockType> = CppBlockType.entries

    override fun collectDescriptors(file: PsiFile): List<DefinitionBlockDescriptor>? {
        debug { "collectDescriptors" }
        val file = file as? CppFile ?: return null
        val collector = BlockCollector()

        file.children.forEach { child ->
            val child = child as? ASTWrapperPsiElement ?: return@forEach

//            if (child.node.elementType == STRUCT_KEYWORD) {
//                debug { child }
//            }
        }

        return null
    }
}

enum class CppBlockType(
    override val label: String,
    override val defaultColor: Color,
) : BlockType {
    STRUCT("Struct", DefaultColor.STRUCT);

    override val key: String get() = "LUX_SH_CPP_BG_$name"
    override val colorKey: ColorKey = ColorKey.createColorKey(key, defaultColor)
}

@Language("C")
private val PREVIEW_TEXT = """
    struct SomeStruct {
        int someInt;
    };
"""