package com.luxalpa.structuredhighlights.languages

import com.intellij.openapi.editor.colors.ColorKey
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.luxalpa.structuredhighlights.BlockType
import com.luxalpa.structuredhighlights.DefinitionBlockDescriptor
import com.luxalpa.structuredhighlights.Descriptor
import com.luxalpa.structuredhighlights.Kind
import com.luxalpa.structuredhighlights.LanguageSupport
import com.jetbrains.php.lang.psi.PhpFile
import com.jetbrains.php.lang.psi.elements.Function
import com.jetbrains.php.lang.psi.elements.GroupStatement
import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.jetbrains.php.lang.psi.elements.PhpNamespace
import com.jetbrains.php.lang.psi.visitors.PhpElementVisitor
import org.intellij.lang.annotations.Language
import java.awt.Color

class Php : LanguageSupport {
    override fun collectDescriptors(file: PsiFile): List<DefinitionBlockDescriptor>? {
        val file = file as? PhpFile ?: return null
        val visitor = PhpVisitor()
        file.accept(visitor)
        return visitor.collector.definitions
    }

    override val blockTypes: List<BlockType> = PhpBlockType.entries.toList()
    override val displayName: String = "PHP"
    override val languageId: String = "PHP"
    override val previewText: String = PREVIEW_TEXT
}

enum class PhpBlockType(
    override val label: String,
    override val defaultColor: Color,
) : BlockType {
    CLASS("Class", DefaultColor.CLASS),
    INTERFACE("Interface", DefaultColor.INTERFACE),
    TRAIT("Trait", DefaultColor.INTERFACE),
    ENUM("Enum", DefaultColor.ENUM),
    FUNCTION("Function", DefaultColor.FUNCTION),
    NAMESPACE("Namespace", DefaultColor.MODULE);

    override val key: String get() = "LUX_SH_PHP_BG_$name"
    override val colorKey: ColorKey = ColorKey.createColorKey(key, defaultColor)
}

class PhpVisitor : PhpElementVisitor() {
    val collector = BlockCollector()

    override fun visitElement(element: PsiElement) {
        element.acceptChildren(this)
    }

    override fun visitPhpNamespace(namespace: PhpNamespace) {
        val descriptors = buildList {
            add(Descriptor(Kind.Block, namespace))
            namespace.nameIdentifier?.let {
                add(Descriptor(Kind.Header, it))
                add(Descriptor(Kind.Identifier, it))
            }
        }
        collector.collect(PhpBlockType.NAMESPACE, descriptors, useForChildren = false) {
            super.visitPhpNamespace(namespace)
        }
    }

    override fun visitPhpClass(phpClass: PhpClass) {
        if (phpClass.isAnonymous) {
            super.visitPhpClass(phpClass)
            return
        }

        val descriptors = buildList {
            add(Descriptor(Kind.Block, phpClass))
            phpClass.nameIdentifier?.let {
                add(Descriptor(Kind.Header, it))
                add(Descriptor(Kind.Identifier, it))
            }
        }

        val blockType = when {
            phpClass.isTrait -> PhpBlockType.TRAIT
            phpClass.isEnum -> PhpBlockType.ENUM
            phpClass.isInterface -> PhpBlockType.INTERFACE
            else -> PhpBlockType.CLASS
        }

        collector.collect(blockType, descriptors) {
            super.visitPhpClass(phpClass)
        }
    }

    override fun visitPhpMethod(method: Method) {
        if (method.isAbstract || !hasBlockBody(method)) {
            super.visitPhpMethod(method)
            return
        }

        val descriptors = buildList {
            method.nameIdentifier?.let {
                add(Descriptor(Kind.Subheader, it))
                add(Descriptor(Kind.Identifier, it))
            }
        }
        collector.collect(PhpBlockType.CLASS, descriptors) {
            super.visitPhpMethod(method)
        }
    }

    override fun visitPhpFunction(function: Function) {
        if (function is Method) {
            super.visitPhpFunction(function)
            return
        }
        if (function.isClosure || !hasBlockBody(function)) {
            super.visitPhpFunction(function)
            return
        }

        val descriptors = buildList {
            add(Descriptor(Kind.Block, function))
            function.nameIdentifier?.let {
                add(Descriptor(if (collector.isTopLevel) Kind.Header else Kind.Subheader, it))
                add(Descriptor(Kind.Identifier, it))
            }
        }
        collector.collect(PhpBlockType.FUNCTION, descriptors) {
            super.visitPhpFunction(function)
        }
    }

    private fun hasBlockBody(function: Function): Boolean {
        return PsiTreeUtil.getChildOfType(function, GroupStatement::class.java) != null
    }
}

@Language("PHP")
private val PREVIEW_TEXT = """
    <?php

    namespace App\Creatures;

    interface Terrible {
        public function breatheFire(): void;
        public function devour(int ${'$'}numPeople): void;
    }

    trait Named {
        public function getName(): string {
            return ${'$'}this->name;
        }
    }

    class Dragon implements Terrible {
        use Named;

        public string ${'$'}name;
        public float ${'$'}age;

        public function roar(): void {
            echo "Roar!!!";
        }

        public function breatheFire(): void {
            echo "Breathing fire!";
            ${'$'}this->roar();
        }

        public function devour(int ${'$'}numPeople): void {
            echo "Devouring ${'$'}numPeople snacks";
            ${'$'}this->roar();
        }
    }

    enum Weapon {
        case TAIL;
        case CLAWS;
        case WINGS;
        case TEETH;
        case FIRE;

        public function attack(): void {
            echo "Attacked with " . ${'$'}this->name;
        }
    }

    function hatch(string ${'$'}name): Dragon {
        ${'$'}dragon = new Dragon();
        ${'$'}dragon->name = ${'$'}name;
        return ${'$'}dragon;
    }
""".trimIndent()
