package com.luxalpa.structuredhighlights.languages

import com.intellij.openapi.editor.colors.ColorKey
import com.intellij.psi.PsiFile
import com.intellij.psi.util.endOffset
import com.intellij.psi.util.startOffset
import com.luxalpa.structuredhighlights.BlockType
import com.luxalpa.structuredhighlights.DefinitionBlockDescriptor
import com.luxalpa.structuredhighlights.Descriptor
import com.luxalpa.structuredhighlights.Kind
import com.luxalpa.structuredhighlights.LanguageSupport
import com.luxalpa.structuredhighlights.Mode
import org.rust.lang.core.psi.RsEnumItem
import org.rust.lang.core.psi.RsFile
import org.rust.lang.core.psi.RsFunction
import org.rust.lang.core.psi.RsImplItem
import org.rust.lang.core.psi.RsModItem
import org.rust.lang.core.psi.RsRecursiveVisitor
import org.rust.lang.core.psi.RsStructItem
import org.rust.lang.core.psi.RsTraitItem
import java.awt.Color

class Rust : LanguageSupport {
    override fun collectDescriptors(file: PsiFile): List<DefinitionBlockDescriptor>? {
        val file = file as? RsFile ?: return null
        val visitor = RustVisitor()
        file.accept(visitor)
        return visitor.definitions
    }

    override val blockTypes: List<BlockType> = RsBlockType.entries.toList()
    override val displayName: String = "Rust"
}

enum class RsBlockType(
    override val label: String,
    override val defaultColor: Color,
) : BlockType {
    ENUM("Enum", Color(-2490113)),
    STRUCT("Struct", Color(-16756225)),
    TRAIT("Trait", Color(-16521928)),
    IMPL("Impl", Color(-20992)),
    FUNCTION("Function", Color(-842752)),
    MODULE("Module", Color(-10066330));

    override val key: String get() = "LUX_SH_RUST_BG_$name"
    override val colorKey: ColorKey = ColorKey.createColorKey(key, defaultColor)
}


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

        handleBlockType(RsBlockType.STRUCT, descriptors) {
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

        handleBlockType(RsBlockType.IMPL, descriptors) {
            super.visitImplItem(o)
        }
    }

    override fun visitFunction(o: RsFunction) {
        val isTopLevel = curBlockType == null
        val descriptors = buildList {
            // Currently, we don't allow blocks inside blocks due to performance issues.
            if (isTopLevel) {
                add(Descriptor(Kind.Block, o))
            }
            o.identifier.let {
                add(Descriptor(if (isTopLevel) Kind.Header else Kind.Subheader, it))
                add(Descriptor(Kind.Identifier, it))
            }
        }

        handleBlockType(RsBlockType.FUNCTION, descriptors) {
            super.visitFunction(o)
        }
    }

    override fun visitModItem(o: RsModItem) {
        // Modules will still be colored like their parents, but freestanding modules will not pass on their
        // color to their children.
        val descriptors = buildList {
//            add(Descriptor(Kind.Block, o))
            o.identifier.let {
                add(Descriptor(Kind.Header, it))
                add(Descriptor(Kind.Identifier, it))
            }
        }

        handleBlockType(RsBlockType.MODULE, descriptors, false) {
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

        handleBlockType(RsBlockType.TRAIT, descriptors) {
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

        handleBlockType(RsBlockType.ENUM, descriptors) {
            super.visitEnumItem(o)
        }
    }
}