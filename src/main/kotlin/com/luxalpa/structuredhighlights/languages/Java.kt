package com.luxalpa.structuredhighlights.languages

import com.intellij.openapi.editor.colors.ColorKey
import com.intellij.psi.JavaRecursiveElementVisitor
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMethod
import com.luxalpa.structuredhighlights.BlockType
import com.luxalpa.structuredhighlights.DefinitionBlockDescriptor
import com.luxalpa.structuredhighlights.Descriptor
import com.luxalpa.structuredhighlights.Kind
import com.luxalpa.structuredhighlights.LanguageSupport
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.psi.KtNamedFunction
import java.awt.Color

class Java : LanguageSupport {
    override fun collectDescriptors(file: PsiFile): List<DefinitionBlockDescriptor>? {
        val file = file as? PsiJavaFile ?: return null
        val visitor = JavaVisitor()
        file.accept(visitor)
        return visitor.collector.definitions
    }

    override val blockTypes: List<BlockType> = JavaBlockType.entries.toList()
    override val displayName: String = "Java"
    override val languageId: String = "JAVA"
    override val previewText: String = PREVIEW_TEXT
}

enum class JavaBlockType(
    override val label: String,
    override val defaultColor: Color,
) : BlockType {
    CLASS("Class", DefaultColor.STRUCT),
    INTERFACE("Interface", DefaultColor.INTERFACE),
    ANNOTATION("Annotation Type", DefaultColor.MACRO_DEFINITION),
    ENUM("Enum", DefaultColor.ENUM);

    override val key: String get() = "LUX_SH_JAVA_BG_$name"
    override val colorKey: ColorKey = ColorKey.createColorKey(key, defaultColor)
}

class JavaVisitor : JavaRecursiveElementVisitor() {
    val collector = BlockCollector()

    override fun visitClass(o: PsiClass) {
        val descriptors = buildList {
            add(Descriptor(Kind.Block, o))
            o.nameIdentifier?.let {
                add(Descriptor(Kind.Header, it))
                add(Descriptor(Kind.Identifier, it))
            }
        }

        val blockType = if (o.isAnnotationType) {
            JavaBlockType.ANNOTATION
        } else if (o.isEnum) {
            JavaBlockType.ENUM
        } else if (o.isInterface) {
            JavaBlockType.INTERFACE
        } else {
            JavaBlockType.CLASS
        }

        collector.collect(
            blockType,
            descriptors
        ) {
            super.visitClass(o)
        }
    }

    override fun visitMethod(o: PsiMethod) {
        if (o.body == null) {
            super.visitMethod(o)
            return
        }

        val descriptors = buildList {
            o.nameIdentifier?.let {
                add(Descriptor(Kind.Subheader, it))
                add(Descriptor(Kind.Identifier, it))
            }
        }
        collector.collect(JavaBlockType.CLASS, descriptors) {
            super.visitMethod(o)
        }
    }
}

@Language("java")
private val PREVIEW_TEXT = """
    @interface Fierce {
        int level() default 1;
    }

    interface Terrible {
        void breatheFire();
        void devour(int numPeople);
    }

    class Dragon implements Terrible {
        public String name;
        public float age;

        public void roar() {
            System.out.println("Roar!!!");
        }

        @Override
        public void breatheFire() {
            System.out.println("Breathing fire!");
            roar();
        }

        @Override
        public void devour(int numPeople) {
            System.out.println("Devouring " + numPeople + " snacks");
            roar();
        }
    }

    enum Weapon {
        TAIL,
        CLAWS,
        WINGS,
        TEETH,
        FIRE;

        public void equip() {
            System.out.println("Equipped " + this);
        }
    }

    record DragonStats(String name, float age) {
        public void describe() {
            System.out.println(name + " is " + age + " years old");
        }
    }
""".trimIndent()