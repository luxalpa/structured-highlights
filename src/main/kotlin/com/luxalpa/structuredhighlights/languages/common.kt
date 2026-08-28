package com.luxalpa.structuredhighlights.languages

import com.intellij.psi.PsiElement
import com.intellij.psi.util.endOffset
import com.intellij.psi.util.startOffset
import com.luxalpa.structuredhighlights.BlockType
import com.luxalpa.structuredhighlights.DefinitionBlockDescriptor
import com.luxalpa.structuredhighlights.Descriptor
import com.luxalpa.structuredhighlights.Kind
import com.luxalpa.structuredhighlights.Mode
import org.rust.openapiext.document
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
    val document = element.containingFile.document ?: return 0
    val range = element.textRange
    val startLine = document.getLineNumber(range.startOffset)
    val endLine = document.getLineNumber(range.endOffset.coerceAtMost(document.textLength))
    return endLine - startLine + 1
}

class DefaultColor {
    companion object {
        // A plain-old-data type
        val STRUCT = Color(-16756225)

        // A block containing method implementations
        val CLASS = Color(-20992)

        // Enumeration: List of instances of a type
        val ENUM = Color(-2490113)

        // A free-standing function
        val FUNCTION = Color(-842752)

        // Some form of submodule or namespace
        val MODULE = Color(-10066330)

        // a type that contains method-stubs which should be implemented or fulfilled somewhere
        val INTERFACE = Color(-16521928)

        // A definition of a meta-programming construct.
        val MACRO_DEFINITION = Color(-10066330)
    }
}