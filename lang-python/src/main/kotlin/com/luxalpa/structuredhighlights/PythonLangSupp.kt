package com.luxalpa.structuredhighlights

import com.intellij.openapi.editor.colors.ColorKey
import com.intellij.psi.PsiFile
import com.jetbrains.python.psi.PyClass
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.psi.PyFunction
import com.jetbrains.python.psi.PyRecursiveElementVisitor
import com.luxalpa.structuredhighlights.shared.BlockCollector
import com.luxalpa.structuredhighlights.shared.BlockType
import com.luxalpa.structuredhighlights.shared.DefaultColor
import com.luxalpa.structuredhighlights.shared.DefinitionBlockDescriptor
import com.luxalpa.structuredhighlights.shared.Descriptor
import com.luxalpa.structuredhighlights.shared.Kind
import com.luxalpa.structuredhighlights.shared.LanguageSupport
import org.intellij.lang.annotations.Language
import java.awt.Color

class Python : LanguageSupport {
    override val languageId: String = "Python"
    override val displayName: String = "Python"
    override val previewText: String = PREVIEW_TEXT
    override val blockTypes: List<BlockType> = PyBlockType.entries

    override fun collectDescriptors(file: PsiFile): List<DefinitionBlockDescriptor>? {
        val file = file as? PyFile ?: return null
        val visitor = PythonVisitor()
        file.accept(visitor)
        return visitor.collector.definitions
    }
}

enum class PyBlockType(
    override val label: String,
    override val defaultColor: Color,
) : BlockType {
    CLASS("Class", DefaultColor.CLASS),
    FUNCTION("Function", DefaultColor.FUNCTION);

    override val key: String get() = "LUX_SH_PYTHON_BG_$name"
    override val colorKey: ColorKey = ColorKey.createColorKey(key, defaultColor)
}

class PythonVisitor : PyRecursiveElementVisitor() {
    val collector = BlockCollector()

    override fun visitPyClass(node: PyClass) {
        collector.collectBlock(PyBlockType.CLASS, node, node.nameIdentifier) {
            super.visitPyClass(node)
        }
    }

    override fun visitPyFunction(node: PyFunction) {
        val descriptors = buildList {
            // Currently, we don't allow blocks inside blocks due to performance issues.
            if (collector.isTopLevel) {
                add(Descriptor(Kind.Block, node))
            }
            node.nameIdentifier?.let {
                add(Descriptor(if (collector.isTopLevel) Kind.Header else Kind.Subheader, it))
                add(Descriptor(Kind.Identifier, it))
            }
        }

        collector.collect(PyBlockType.FUNCTION, descriptors) {
            super.visitPyFunction(node)
        }
    }
}

@Language("Python")
private val PREVIEW_TEXT = """
    class Dragon:
        def __init__(self, name):
            self.name = name

        def roar(self):
            print("Roar!!!")

        def breathe_fire(self):
            print("Breathing fire!")
            self.roar()


    class Hungry:
        def devour(self, num_people):
            print(f"Devouring {num_people} snacks")


    def hatch(name):
        dragon = Dragon(name)
        return dragon
""".trimIndent()
