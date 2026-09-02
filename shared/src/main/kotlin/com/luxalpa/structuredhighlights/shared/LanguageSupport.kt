package com.luxalpa.structuredhighlights.shared

import com.intellij.openapi.editor.colors.ColorKey
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
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

enum class Mode {
    FULL_LINE,
    EXACT_RANGE;
}

data class DefinitionBlockDescriptor(
    val startOffset: Int,
    val endOffset: Int,
    val blockType: BlockType,

    // Currently unused, might be reenabled later though.
    // This is the block type it would be if it wasn't overridden by the higher-level block.
    val actualType: BlockType,

    val kind: Kind = Kind.Block,
    val mode: Mode = Mode.FULL_LINE
)

enum class Kind {
    Block, Header, Subheader, Identifier;

    // The renderer draws these from high to low for some reason.
    val layer: Int
        get() = when (this) {
            Block -> 2
            Header -> 1
            Subheader -> 1
            Identifier -> 0
        }
}

data class Descriptor(val kind: Kind, val element: PsiElement)