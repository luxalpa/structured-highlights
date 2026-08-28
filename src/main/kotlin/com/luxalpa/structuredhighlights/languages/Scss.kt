package com.luxalpa.structuredhighlights.languages

import com.intellij.openapi.editor.colors.ColorKey
import com.intellij.psi.PsiFile
import com.intellij.psi.css.CssKeyframesRule
import com.intellij.psi.css.CssMedia
import com.intellij.psi.css.CssRuleset
import com.luxalpa.structuredhighlights.BlockType
import com.luxalpa.structuredhighlights.DefinitionBlockDescriptor
import com.luxalpa.structuredhighlights.Descriptor
import com.luxalpa.structuredhighlights.Kind
import com.luxalpa.structuredhighlights.LanguageSupport
import org.intellij.lang.annotations.Language
import org.jetbrains.plugins.scss.psi.SCSSFile
import org.jetbrains.plugins.scss.psi.SCSSMixinDeclaration
import org.jetbrains.plugins.scss.psi.SassScssFunctionDeclaration
import java.awt.Color

class Scss : LanguageSupport {
    override val blockTypes: List<BlockType> = ScssBlockType.entries
    override val displayName: String = "SCSS"
    override val languageId: String = "SCSS"
    override val previewText: String = PREVIEW_TEXT

    override fun collectDescriptors(file: PsiFile): List<DefinitionBlockDescriptor>? {
        val file = file as? SCSSFile ?: return null

        val collector = BlockCollector()

        file.stylesheet.rulesetList.children.forEach { child ->
            when (child) {
                is SCSSMixinDeclaration -> {
                    collector.collectBlock(ScssBlockType.MIXIN, child, child.nameIdentifier)
                }

                is SassScssFunctionDeclaration -> {
                    collector.collectBlock(ScssBlockType.FUNCTION, child, child.nameIdentifier)
                }
                
                else -> {
                    collector.collectCssChild(child)
                }
            }
        }

        return collector.definitions
    }
}

enum class ScssBlockType(
    override val label: String,
    override val defaultColor: Color,
) : BlockType {
    MIXIN("Mixin", DefaultColor.CLASS),
    FUNCTION("Function", DefaultColor.FUNCTION);

    override val key: String = "LUX_SH_SCSS_BG_$name"
    override val colorKey: ColorKey = ColorKey.createColorKey(key, defaultColor)
}

@Language("SCSS")
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
    
    @mixin territory {
        margin: 1000px;
    }
    
    @function roar() {
        @return "ROARRR!!";
    }
    
""".trimIndent()