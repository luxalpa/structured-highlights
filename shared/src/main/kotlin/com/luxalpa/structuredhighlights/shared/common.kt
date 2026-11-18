package com.luxalpa.structuredhighlights.shared

import com.intellij.openapi.editor.colors.ColorKey
import com.intellij.psi.PsiElement
import com.intellij.psi.util.endOffset
import com.intellij.psi.util.startOffset
import java.awt.Color

// For handling nesting.
class BlockCollector {
    var curBlockType: BlockType? = null
    val definitions = mutableListOf<DefinitionBlockDescriptor>()

    val isTopLevel: Boolean get() = curBlockType == null

    fun collect(
        defaultType: BlockType,
        descriptors: List<Descriptor>,
        useForChildren: Boolean = true,
        visit: () -> Unit = {}
    ) {
        val isTopLevel = curBlockType == null
        val newBlockType = curBlockType ?: defaultType
        if (useForChildren) {
            curBlockType = newBlockType
        }

        for (descriptor in descriptors) {
            val mode = when (descriptor.kind) {
                Kind.Block, Kind.Header, Kind.Subheader -> Mode.FULL_LINE
                Kind.Identifier -> Mode.EXACT_RANGE
            }

            definitions += DefinitionBlockDescriptor(
                descriptor.element.startOffset,
                descriptor.element.endOffset,
                newBlockType,
                defaultType,
                descriptor.kind,
                mode
            )
        }

        visit()
        if (isTopLevel) curBlockType = null
    }

    fun collectBlock(
        type: BlockType,
        block: PsiElement? = null,
        name: PsiElement? = null,
        headerKind: Kind = if (isTopLevel) Kind.Header else Kind.Subheader,
        useForChildren: Boolean = true,
        visit: () -> Unit = {}
    ) = collect(type, buildList {
        block?.let { add(Descriptor(Kind.Block, it)) }
        name?.let {
            add(Descriptor(headerKind, it))
            add(Descriptor(Kind.Identifier, it))
        }
    }, useForChildren, visit)
}

fun lineSpan(element: PsiElement): Int {
    val document = element.containingFile.viewProvider.document ?: return 0
    val range = element.textRange
    val startLine = document.getLineNumber(range.startOffset)
    val endLine = document.getLineNumber(range.endOffset.coerceAtMost(document.textLength))
    return endLine - startLine + 1
}

/**
 * The base colors shown under "General" on the color settings page. Every language-specific block color inherits from
 * one of these (via a fallback [ColorKey]) unless the user disables the inheritance for it.
 */
enum class BaseColor(val label: String, val defaultColor: Color) {
    // A plain-old-data type
    STRUCT("Struct", Color(-16756225)),

    // A block containing method implementations
    CLASS("Class", Color(-20992)),

    // Enumeration: List of instances of a type
    ENUM("Enum", Color(-2490113)),

    // A free-standing function
    FUNCTION("Function", Color(-842752)),

    // Some form of submodule or namespace
    MODULE("Module", Color(-10066330)),

    // a type that contains method-stubs which should be implemented or fulfilled somewhere
    INTERFACE("Interface", Color(-16521928)),

    // A definition of a meta-programming construct.
    MACRO_DEFINITION("Macro Definition", Color(-10066330));

    val colorKey: ColorKey = ColorKey.createColorKey("LUX_SH_BASE_$name", defaultColor)
}

/**
 * Creates the color key for a language-specific block type that inherits from [base].
 *
 * The key intentionally has no default color of its own: the color scheme prefers a key's own default over its
 * fallback's, so a default here would break inheritance.
 */
fun blockColorKey(externalName: String, base: BaseColor): ColorKey =
    ColorKey.createColorKeyWithFallback(externalName, base.colorKey)