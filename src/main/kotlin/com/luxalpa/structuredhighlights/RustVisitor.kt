package com.luxalpa.structuredhighlights

import com.intellij.psi.util.endOffset
import com.intellij.psi.util.startOffset
import org.rust.lang.core.psi.RsEnumItem
import org.rust.lang.core.psi.RsFunction
import org.rust.lang.core.psi.RsImplItem
import org.rust.lang.core.psi.RsModItem
import org.rust.lang.core.psi.RsRecursiveVisitor
import org.rust.lang.core.psi.RsStructItem
import org.rust.lang.core.psi.RsTraitItem


class RustVisitor : RsRecursiveVisitor() {
    val definitions = mutableListOf<DefinitionBlockDescriptor>()

    var curBlockType: BlockType? = null

    private fun handleBlockType(
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

    override fun visitStructItem(o: RsStructItem) {
        val descriptors = buildList {
            add(Descriptor(Kind.Block, o))
            o.identifier?.let {
                add(Descriptor(Kind.Header, it))
                add(Descriptor(Kind.Identifier, it))
            }
        }

        handleBlockType(BlockType.STRUCT, descriptors) {
            super.visitStructItem(o)
        }
    }

    override fun visitImplItem(o: RsImplItem) {
        val descriptors = buildList {
            add(Descriptor(Kind.Block, o))
            o.typeReference?.let {
                add(Descriptor(Kind.Header, it))
                add(Descriptor(Kind.Identifier, it))
            }
            o.traitRef?.let {
                add(Descriptor(Kind.Identifier, it))
            }
        }

        handleBlockType(BlockType.IMPL, descriptors) {
            super.visitImplItem(o)
        }
    }

    override fun visitFunction(o: RsFunction) {
        val isTopLevel = curBlockType == null
        val descriptors = buildList {
            add(Descriptor(Kind.Block, o))
            o.identifier.let {
                add(Descriptor(if (isTopLevel) Kind.Header else Kind.Subheader, it))
                add(Descriptor(Kind.Identifier, it))
            }
        }

        handleBlockType(BlockType.FUNCTION, descriptors) {
            super.visitFunction(o)
        }
    }

    override fun visitModItem(o: RsModItem) {
        // Modules will still be colored like their parents, but freestanding modules will not pass on their
        // color to their children.
        val descriptors = buildList {
            add(Descriptor(Kind.Block, o))
            o.identifier.let {
                add(Descriptor(Kind.Header, it))
                add(Descriptor(Kind.Identifier, it))
            }
        }

        handleBlockType(BlockType.MODULE, descriptors, false) {
            super.visitModItem(o)
        }
    }

    override fun visitTraitItem(o: RsTraitItem) {
        val descriptors = buildList {
            add(Descriptor(Kind.Block, o))
            o.identifier?.let {
                add(Descriptor(Kind.Header, it))
                add(Descriptor(Kind.Identifier, it))
            }
        }

        handleBlockType(BlockType.TRAIT, descriptors) {
            super.visitTraitItem(o)
        }
    }

    override fun visitEnumItem(o: RsEnumItem) {
        val descriptors = buildList {
            add(Descriptor(Kind.Block, o))
            o.identifier?.let {
                add(Descriptor(Kind.Header, it))
                add(Descriptor(Kind.Identifier, it))
            }
        }

        handleBlockType(BlockType.ENUM, descriptors) {
            super.visitEnumItem(o)
        }
    }
}