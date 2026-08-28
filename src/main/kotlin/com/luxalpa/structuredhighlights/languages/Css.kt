package com.luxalpa.structuredhighlights.languages

import com.intellij.openapi.editor.colors.ColorKey
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.css.CssFile
import com.intellij.psi.css.CssKeyframesRule
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
    override val languageId: String = "CSS"
    override val displayName: String = "CSS"
    override val previewText: String = PREVIEW_TEXT
    override val blockTypes: List<BlockType> = CssBlockType.entries

    override fun collectDescriptors(file: PsiFile): List<DefinitionBlockDescriptor>? {
        val file = file as? CssFile ?: return null

        val collector = BlockCollector()

        file.stylesheet.rulesetList.children.forEach { child ->
            collector.collectCssChild(child)
        }

        return collector.definitions
    }
}

fun BlockCollector.collectCssChild(child: PsiElement) {
    when (child) {
        is CssRuleset -> {
            collectBlock(CssBlockType.RULESET, child, child.selectorList)
        }

        is CssMedia -> {
            collectBlock(CssBlockType.MEDIA, child, child.mediumList)
        }

        is CssKeyframesRule -> {
            collectBlock(CssBlockType.KEYFRAMES, child, child.nameIdentifier)
        }
    }
}

enum class CssBlockType(
    override val label: String,
    override val defaultColor: Color,
) : BlockType {
    RULESET("Ruleset", DefaultColor.STRUCT),
    MEDIA("Media Query", DefaultColor.STRUCT),
    KEYFRAMES("Keyframes", DefaultColor.ENUM);

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