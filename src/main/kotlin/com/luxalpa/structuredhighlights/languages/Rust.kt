package com.luxalpa.structuredhighlights.languages

import com.intellij.openapi.editor.colors.ColorKey
import com.intellij.psi.PsiFile
import com.luxalpa.structuredhighlights.BlockType
import com.luxalpa.structuredhighlights.DefinitionBlockDescriptor
import com.luxalpa.structuredhighlights.Descriptor
import com.luxalpa.structuredhighlights.Kind
import com.luxalpa.structuredhighlights.LanguageSupport
import org.intellij.lang.annotations.Language
import org.rust.lang.core.psi.RsEnumItem
import org.rust.lang.core.psi.RsFile
import org.rust.lang.core.psi.RsFunction
import org.rust.lang.core.psi.RsImplItem
import org.rust.lang.core.psi.RsMacro
import org.rust.lang.core.psi.RsModItem
import org.rust.lang.core.psi.RsRecursiveVisitor
import org.rust.lang.core.psi.RsStructItem
import org.rust.lang.core.psi.RsTraitItem
import org.rust.lang.core.psi.ext.body
import java.awt.Color

class Rust : LanguageSupport {
    override fun collectDescriptors(file: PsiFile): List<DefinitionBlockDescriptor>? {
        val file = file as? RsFile ?: return null
        val visitor = RustVisitor()
        file.accept(visitor)
        return visitor.collector.definitions
    }

    override val blockTypes: List<BlockType> = RsBlockType.entries.toList()
    override val displayName: String = "Rust"
    override val languageId: String = "Rust"
    override val previewText: String = PREVIEW_TEXT
}

enum class RsBlockType(
    override val label: String,
    override val defaultColor: Color,
) : BlockType {
    ENUM("Enum", DefaultColor.ENUM),
    STRUCT("Struct", DefaultColor.STRUCT),
    TRAIT("Trait", DefaultColor.INTERFACE),
    IMPL("Impl", DefaultColor.CLASS),
    FUNCTION("Function", DefaultColor.FUNCTION),
    MODULE("Module", DefaultColor.MODULE),
    MACRORULES("Macro Definition", DefaultColor.MACRO_DEFINITION);

    override val key: String get() = "LUX_SH_RUST_BG_$name"
    override val colorKey: ColorKey = ColorKey.createColorKey(key, defaultColor)
}

class RustVisitor : RsRecursiveVisitor() {
    val collector = BlockCollector()

    override fun visitStructItem(o: RsStructItem) {
        collector.collectBlock(RsBlockType.STRUCT, o, o.identifier) {
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

        collector.collect(RsBlockType.IMPL, descriptors) {
            super.visitImplItem(o)
        }
    }

    override fun visitFunction(o: RsFunction) {
        if (o.body == null) {
            super.visitFunction(o)
            return
        }

        val descriptors = buildList {
            // Currently, we don't allow blocks inside blocks due to performance issues.
            if (collector.isTopLevel) {
                add(Descriptor(Kind.Block, o))
            }
            o.identifier.let {
                add(Descriptor(if (collector.isTopLevel) Kind.Header else Kind.Subheader, it))
                add(Descriptor(Kind.Identifier, it))
            }
        }

        collector.collect(RsBlockType.FUNCTION, descriptors) {
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

        collector.collectBlock(RsBlockType.MODULE, o, o.identifier, useForChildren = false) {
            super.visitModItem(o)
        }
    }

    override fun visitTraitItem(o: RsTraitItem) {
        collector.collectBlock(RsBlockType.TRAIT, o, o.identifier) {
            super.visitTraitItem(o)
        }
    }

    override fun visitEnumItem(o: RsEnumItem) {
        collector.collectBlock(RsBlockType.ENUM, o, o.identifier) {
            super.visitEnumItem(o)
        }
    }

    override fun visitMacro(o: RsMacro) {
        collector.collectBlock(RsBlockType.MACRORULES, o, o.nameIdentifier) {
            super.visitMacro(o)
        }
    }
}

@Language("Rust")
private val PREVIEW_TEXT = """
    trait Terrible {
        fn breathe_fire(&self);
        fn devour(&self, num_people: usize);
    }

    #[derive(Clone, Debug)]
    struct Dragon {
        pub name: String,
        pub age: f32
    }

    impl Dragon {
        pub fn roar(&self) {
            println!("Roar!!!");
        }
    }

    impl Terrible for Dragon {
        fn breathe_fire(&self) {
            println!("Breathing fire!");
            self.roar();
        }
        
        fn devour(&self, num_people: usize) {
            println!("Devouring {} snacks", num_people);
            self.roar();
        }
    }

    enum Weapon {
        Tail,
        Claws { num_talons: usize },
        Wings,
        Teeth(usize),
        Fire,
    }
    
    macro_rules! wake_dragon {
        () => {
            Dragon {
                name: "Bahamut".to_string(),
                age: 7000.0,
            }
        };
    }

    #[cfg(test)]
    mod tests {
        use super::*;

        fn test_dragon() {
            let dragon = Dragon {
                name: "Smaug".to_string(),
                age: 7000.0,
            };

            dragon.breathe_fire();
        }
    }
""".trimIndent()
