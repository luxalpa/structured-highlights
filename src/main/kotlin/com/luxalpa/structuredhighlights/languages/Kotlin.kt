package com.luxalpa.structuredhighlights.languages

import com.intellij.openapi.editor.colors.ColorKey
import com.intellij.psi.PsiFile
import com.luxalpa.structuredhighlights.BlockType
import com.luxalpa.structuredhighlights.DefinitionBlockDescriptor
import com.luxalpa.structuredhighlights.Descriptor
import com.luxalpa.structuredhighlights.Kind
import com.luxalpa.structuredhighlights.LanguageSupport
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import java.awt.Color

class Kotlin : LanguageSupport {
    override fun collectDescriptors(file: PsiFile): List<DefinitionBlockDescriptor>? {
        val file = file as? KtFile ?: return null
        val visitor = KotlinVisitor()
        file.accept(visitor)
        return visitor.collector.definitions
    }

    override val blockTypes: List<BlockType> = KtBlockType.entries.toList()
    override val displayName: String = "Kotlin"
    override val languageId: String = "kotlin"
    override val previewText: String = PREVIEW_TEXT
}

enum class KtBlockType(
    override val label: String,
    override val defaultColor: Color,
) : BlockType {
    CLASS("Class", DefaultColor.CLASS),
    FUNCTION("Function", DefaultColor.FUNCTION),
    INTERFACE("Interface", DefaultColor.INTERFACE),
    ENUM("Enum", DefaultColor.ENUM),
    OBJECT("Object", DefaultColor.CLASS);

    override val key: String get() = "LUX_SH_KOTLIN_BG_$name"
    override val colorKey: ColorKey = ColorKey.createColorKey(key, defaultColor)
}

class KotlinVisitor : KtTreeVisitorVoid() {
    val collector = BlockCollector()

    override fun visitClass(o: KtClass) {
        if (o is KtEnumEntry) {
            super.visitClass(o)
            return
        }

        val descriptors = buildList {
            add(Descriptor(Kind.Block, o))
            o.nameIdentifier?.let {
                add(Descriptor(Kind.Header, it))
                add(Descriptor(Kind.Identifier, it))
            }
        }

        collector.collect(
            if (o.isEnum()) KtBlockType.ENUM else if (o.isInterface()) KtBlockType.INTERFACE else KtBlockType.CLASS,
            descriptors
        ) {
            super.visitClass(o)
        }
    }

    override fun visitObjectDeclaration(o: KtObjectDeclaration) {
        val descriptors = buildList {
            add(Descriptor(Kind.Block, o))
            o.nameIdentifier?.let {
                add(Descriptor(Kind.Header, it))
                add(Descriptor(Kind.Identifier, it))
            }
        }
        collector.collect(KtBlockType.OBJECT, descriptors) {
            super.visitObjectDeclaration(o)
        }
    }

    override fun visitNamedFunction(o: KtNamedFunction) {
        if (!o.hasBody() || !o.hasBlockBody()) {
            super.visitNamedFunction(o)
            return
        }

        val descriptors = buildList {
            add(Descriptor(Kind.Block, o))
            o.nameIdentifier?.let {
                add(Descriptor(if (collector.isTopLevel) Kind.Header else Kind.Subheader, it))
                add(Descriptor(Kind.Identifier, it))
            }
        }
        collector.collect(KtBlockType.FUNCTION, descriptors) {
            super.visitNamedFunction(o)
        }
    }
}

@Language("kotlin")
private val PREVIEW_TEXT = """
    class Dragon {
        fun roar() {
            println("Roar!!!")
        }
    }

    interface Greedy {
        fun gatherTreasure()
    }

    enum class Color {
        RED
    }

    fun main() {
        Dragon().roar()
    }
""".trimIndent()