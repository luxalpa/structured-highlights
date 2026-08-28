package com.luxalpa.structuredhighlights.languages

import com.intellij.openapi.editor.colors.ColorKey
import com.intellij.psi.PsiFile
import com.luxalpa.structuredhighlights.BlockType
import com.luxalpa.structuredhighlights.DefinitionBlockDescriptor
import com.luxalpa.structuredhighlights.Descriptor
import com.luxalpa.structuredhighlights.Kind
import com.luxalpa.structuredhighlights.LanguageSupport
import org.intellij.lang.annotations.Language
import org.toml.lang.psi.TomlFile
import org.toml.lang.psi.TomlRecursiveVisitor
import org.toml.lang.psi.TomlTable
import java.awt.Color

class Toml : LanguageSupport {
    override val languageId: String = "TOML"
    override val displayName: String = "TOML"
    override val previewText: String = PREVIEW_TEXT
    override val blockTypes: List<BlockType> = TomlBlockType.entries

    override fun collectDescriptors(file: PsiFile): List<DefinitionBlockDescriptor>? {
        val file = file as? TomlFile ?: return null
        val visitor = TomlVisitor()
        file.accept(visitor)
        return visitor.collector.definitions
    }
}

enum class TomlBlockType(
    override val label: String,
    override val defaultColor: Color,
) : BlockType {
    TABLE("Table", DefaultColor.STRUCT);

    override val key: String get() = "LUX_SH_TOML_BG_$name"
    override val colorKey: ColorKey = ColorKey.createColorKey(key, defaultColor)
}

// TODO: Consider just loading the list of Tables instead of using a visitor.
class TomlVisitor : TomlRecursiveVisitor() {
    val collector = BlockCollector()

    override fun visitTable(element: TomlTable) {

        val descriptors = buildList {
            // Currently, we don't allow blocks inside blocks due to performance issues.
            if (collector.isTopLevel) {
                add(Descriptor(Kind.Block, element))
            }
            element.header.let { header ->
                add(Descriptor(Kind.Header, header))
                // TOML Table Headers already make the text very visible,
                // so the extra identifier might not be needed.
//                header.key?.let {
//                    add(Descriptor(Kind.Identifier, it))
//                }
            }
        }

        collector.collect(TomlBlockType.TABLE, descriptors) {
            super.visitTable(element)
        }
    }
}

@Language("TOML")
private val PREVIEW_TEXT: String = """
    [dragon]
    name = "Atarka"
    origin = "Tarkir"
    colors = [
        "red", 
        "green"
    ]
""".trimIndent()