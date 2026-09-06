package com.luxalpa.structuredhighlights

import com.goide.psi.GoFile
import com.goide.psi.GoFunctionDeclaration
import com.goide.psi.GoInterfaceType
import com.goide.psi.GoMethodDeclaration
import com.goide.psi.GoRecursiveVisitor
import com.goide.psi.GoStructType
import com.goide.psi.GoTypeSpec
import com.intellij.openapi.editor.colors.ColorKey
import com.intellij.psi.PsiFile
import com.luxalpa.structuredhighlights.shared.BlockCollector
import com.luxalpa.structuredhighlights.shared.BlockType
import com.luxalpa.structuredhighlights.shared.DefaultColor
import com.luxalpa.structuredhighlights.shared.DefinitionBlockDescriptor
import com.luxalpa.structuredhighlights.shared.Descriptor
import com.luxalpa.structuredhighlights.shared.Kind
import com.luxalpa.structuredhighlights.shared.LanguageSupport
import org.intellij.lang.annotations.Language
import java.awt.Color

class Go : LanguageSupport {
    override val languageId: String = "go"
    override val displayName: String = "Go"
    override val previewText: String = PREVIEW_TEXT
    override val blockTypes: List<BlockType> = GoBlockType.entries

    override fun collectDescriptors(file: PsiFile): List<DefinitionBlockDescriptor>? {
        val file = file as? GoFile ?: return null
        val visitor = GoVisitor()
        file.accept(visitor)
        return visitor.collector.definitions
    }
}

enum class GoBlockType(
    override val label: String,
    override val defaultColor: Color,
) : BlockType {
    STRUCT("Struct", DefaultColor.STRUCT),
    INTERFACE("Interface", DefaultColor.INTERFACE),
    FUNCTION("Function", DefaultColor.FUNCTION);

    override val key: String get() = "LUX_SH_GO_BG_$name"
    override val colorKey: ColorKey = ColorKey.createColorKey(key, defaultColor)
}

class GoVisitor : GoRecursiveVisitor() {
    val collector = BlockCollector()

    override fun visitTypeSpec(o: GoTypeSpec) {
        val blockType = when (o.specType.type) {
            is GoStructType -> GoBlockType.STRUCT
            is GoInterfaceType -> GoBlockType.INTERFACE
            else -> {
                super.visitTypeSpec(o)
                return
            }
        }

        collector.collectBlock(blockType, o, o.identifier) {
            super.visitTypeSpec(o)
        }
    }

    override fun visitFunctionDeclaration(o: GoFunctionDeclaration) {
        if (o.block == null) {
            super.visitFunctionDeclaration(o)
            return
        }
        collectFunction(o, o.identifier) {
            super.visitFunctionDeclaration(o)
        }
    }

    override fun visitMethodDeclaration(o: GoMethodDeclaration) {
        if (o.block == null) {
            super.visitMethodDeclaration(o)
            return
        }
        collectFunction(o, o.identifier) {
            super.visitMethodDeclaration(o)
        }
    }

    private fun collectFunction(
        element: com.intellij.psi.PsiElement,
        name: com.intellij.psi.PsiElement?,
        visit: () -> Unit
    ) {
        val descriptors = buildList {
            // Currently, we don't allow blocks inside blocks due to performance issues.
            if (collector.isTopLevel) {
                add(Descriptor(Kind.Block, element))
            }
            name?.let {
                add(Descriptor(if (collector.isTopLevel) Kind.Header else Kind.Subheader, it))
                add(Descriptor(Kind.Identifier, it))
            }
        }

        collector.collect(GoBlockType.FUNCTION, descriptors, visit = visit)
    }
}

@Language("go")
private val PREVIEW_TEXT = """
    package creatures

    import "fmt"

    type Terrible interface {
        BreatheFire()
        Devour(numPeople int)
    }

    type Dragon struct {
        Name string
        Age  float32
    }

    func (d *Dragon) Roar() {
        fmt.Println("Roar!!!")
    }

    func (d *Dragon) BreatheFire() {
        fmt.Println("Breathing fire!")
        d.Roar()
    }

    func Hatch(name string) *Dragon {
        dragon := &Dragon{Name: name}
        return dragon
    }
""".trimIndent()
