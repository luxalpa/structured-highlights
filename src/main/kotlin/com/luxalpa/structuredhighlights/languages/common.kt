package com.luxalpa.structuredhighlights.languages

import com.intellij.psi.util.endOffset
import com.intellij.psi.util.startOffset
import com.luxalpa.structuredhighlights.BlockType
import com.luxalpa.structuredhighlights.DefinitionBlockDescriptor
import com.luxalpa.structuredhighlights.Descriptor
import com.luxalpa.structuredhighlights.Kind
import com.luxalpa.structuredhighlights.Mode
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
        visit: () -> Unit
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