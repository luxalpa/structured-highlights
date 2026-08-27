package com.luxalpa.structuredhighlights.languages

import com.intellij.openapi.editor.colors.ColorKey
import com.intellij.psi.PsiFile
import com.intellij.psi.css.CssFile
import com.intellij.psi.css.CssMedia
import com.intellij.psi.css.CssRuleset
import com.luxalpa.structuredhighlights.BlockType
import com.luxalpa.structuredhighlights.DefinitionBlockDescriptor
import com.luxalpa.structuredhighlights.Descriptor
import com.luxalpa.structuredhighlights.Kind
import com.luxalpa.structuredhighlights.LanguageSupport
import org.intellij.lang.annotations.Language
import java.awt.Color

class Css : LanguageSupport {
    override fun collectDescriptors(file: PsiFile): List<DefinitionBlockDescriptor>? {
        val file = file as? CssFile ?: return null

        val collector = BlockCollector()

        file.stylesheet.rulesetList.children.forEach { child ->
            when (child) {
                is CssRuleset -> {
                    val descriptors = buildList {
                        add(Descriptor(Kind.Block, child))
                        child.selectorList?.let {
                            add(Descriptor(Kind.Header, it))
                            add(Descriptor(Kind.Identifier, it))
                        }
                    }

                    collector.collect(CssBlockType.RULESET, descriptors)
                }

                is CssMedia -> {
                    val descriptors = buildList {
                        add(Descriptor(Kind.Block, child))
                        child.mediumList?.let {
                            add(Descriptor(Kind.Header, it))
                            add(Descriptor(Kind.Identifier, it))
                        }
                    }

                    collector.collect(CssBlockType.MEDIA, descriptors)
                }
            }
        }

        return collector.definitions
    }

    override val blockTypes: List<BlockType> = CssBlockType.entries
    override val displayName: String = "CSS"
    override val languageId: String = "CSS"
    override val previewText: String = PREVIEW_TEXT
}

enum class CssBlockType(
    override val label: String,
    override val defaultColor: Color,
) : BlockType {
    RULESET("Ruleset", DefaultColor.STRUCT),
    MEDIA("Media Query", DefaultColor.STRUCT);

    override val key: String = "LUX_SH_CSS_BG_$name"
    override val colorKey: ColorKey = ColorKey.createColorKey(key, defaultColor)
}

@Language("CSS")
private val PREVIEW_TEXT = """
    @media (width > 100px) {
        body {
            color: green;
        }
    }
    
    body .dragon {
        color: red;
        pointer-events: none;
    }
""".trimIndent()