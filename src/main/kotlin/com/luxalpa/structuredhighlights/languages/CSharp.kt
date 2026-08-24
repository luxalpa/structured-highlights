package com.luxalpa.structuredhighlights.languages

import com.intellij.openapi.editor.colors.ColorKey
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiRecursiveElementVisitor
import com.luxalpa.structuredhighlights.BlockType
import com.luxalpa.structuredhighlights.DefinitionBlockDescriptor
import com.luxalpa.structuredhighlights.Descriptor
import com.luxalpa.structuredhighlights.Kind
import com.luxalpa.structuredhighlights.LanguageSupport
import org.intellij.lang.annotations.Language
import java.awt.Color

// Rider uses Resharper / Language Server Protocol for C#. The AI generated this stuff. Don't look at it too much!

class CSharp : LanguageSupport {
    override fun collectDescriptors(file: PsiFile): List<DefinitionBlockDescriptor>? {
        if (file.language.id != LANGUAGE_ID) return null
        val visitor = CSharpVisitor()
        file.accept(visitor)
        return visitor.collector.definitions
    }

    override val blockTypes: List<BlockType> = CSharpBlockType.entries.toList()
    override val displayName: String = "C#"
    override val languageId: String = LANGUAGE_ID
    override val previewText: String = PREVIEW_TEXT

    companion object {
        const val LANGUAGE_ID = "C#"
    }
}

enum class CSharpBlockType(
    override val label: String,
    override val defaultColor: Color,
) : BlockType {
    CLASS("Class", DefaultColor.CLASS),
    STRUCT("Struct", DefaultColor.STRUCT),
    INTERFACE("Interface", DefaultColor.INTERFACE),
    ENUM("Enum", DefaultColor.ENUM),
    RECORD("Record", DefaultColor.STRUCT),
    FUNCTION("Function", DefaultColor.FUNCTION),
    NAMESPACE("Namespace", DefaultColor.MODULE);

    override val key: String get() = "LUX_SH_CSHARP_BG_$name"
    override val colorKey: ColorKey = ColorKey.createColorKey(key, defaultColor)
}

class CSharpVisitor : PsiRecursiveElementVisitor() {
    val collector = BlockCollector()

    override fun visitElement(element: PsiElement) {
        when (elementTypeName(element)) {
            "cs:class-declaration" -> visitType(element, CSharpBlockType.CLASS)
            "cs:struct-declaration" -> visitType(element, CSharpBlockType.STRUCT)
            "cs:interface-declaration" -> visitType(element, CSharpBlockType.INTERFACE)
            "cs:enum-declaration" -> visitType(element, CSharpBlockType.ENUM)
            "cs:record-declaration", "cs:struct-record-declaration" -> visitType(element, CSharpBlockType.RECORD)
            "cs:namespace-block-declaration" -> visitNamespace(element)
            "cs:method-declaration", "cs:ctor-declaration" -> visitMethod(element)
            else -> super.visitElement(element)
        }
    }

    private fun visitType(element: PsiElement, blockType: CSharpBlockType) {
        val descriptors = buildList {
            add(Descriptor(Kind.Block, element))
            nameElement(element)?.let {
                add(Descriptor(Kind.Header, it))
                add(Descriptor(Kind.Identifier, it))
            }
        }
        collector.collect(blockType, descriptors) {
            super.visitElement(element)
        }
    }

    private fun visitNamespace(element: PsiElement) {
        val descriptors = buildList {
            add(Descriptor(Kind.Block, element))
            nameElement(element)?.let {
                add(Descriptor(Kind.Header, it))
                add(Descriptor(Kind.Identifier, it))
            }
        }
        collector.collect(CSharpBlockType.NAMESPACE, descriptors, useForChildren = false) {
            super.visitElement(element)
        }
    }

    private fun visitMethod(element: PsiElement) {
        if (!hasBlockBody(element)) {
            super.visitElement(element)
            return
        }

        val descriptors = buildList {
            if (collector.isTopLevel) {
                add(Descriptor(Kind.Block, element))
            }
            nameElement(element)?.let {
                add(Descriptor(if (collector.isTopLevel) Kind.Header else Kind.Subheader, it))
                add(Descriptor(Kind.Identifier, it))
            }
        }
        collector.collect(CSharpBlockType.FUNCTION, descriptors) {
            super.visitElement(element)
        }
    }

    private fun hasBlockBody(element: PsiElement): Boolean {
        return element.children.any { elementTypeName(it) == "cs:block-list" }
    }

    private fun nameElement(element: PsiElement): PsiElement? {
        (element as? PsiNameIdentifierOwner)?.nameIdentifier?.let { return it }

        for (child in element.children) {
            when (elementTypeName(child)) {
                "cs:id-role", "REFERENCE_NAME", "DECLARATION_IDENTIFIER" -> return child
            }
        }
        return null
    }

    private fun elementTypeName(element: PsiElement): String {
        return element.node.elementType.toString()
    }
}

@Language("C#")
private val PREVIEW_TEXT = """
    namespace Creatures
    {
        interface ITerrible
        {
            void BreatheFire();
            void Devour(int numPeople);
        }

        class Dragon : ITerrible
        {
            public string Name;
            public float Age;

            public void Roar()
            {
                Console.WriteLine("Roar!!!");
            }

            public void BreatheFire()
            {
                Console.WriteLine("Breathing fire!");
                Roar();
            }

            public void Devour(int numPeople)
            {
                Console.WriteLine($"Devouring {numPeople} snacks");
                Roar();
            }
        }

        enum Weapon
        {
            Tail,
            Claws,
            Wings,
            Teeth,
            Fire
        }

        struct DragonStats
        {
            public string Name;
            public float Age;
        }

        record DragonRecord(string Name, float Age);

        static void Hatch(string name)
        {
            var dragon = new Dragon();
            dragon.Name = name;
        }
    }
""".trimIndent()
