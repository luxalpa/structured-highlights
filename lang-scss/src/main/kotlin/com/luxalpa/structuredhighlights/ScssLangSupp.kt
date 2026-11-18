package com.luxalpa.structuredhighlights

import com.intellij.openapi.editor.colors.ColorKey
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNameIdentifierOwner
import com.luxalpa.structuredhighlights.shared.BlockCollector
import com.luxalpa.structuredhighlights.shared.BlockType
import com.luxalpa.structuredhighlights.shared.BaseColor
import com.luxalpa.structuredhighlights.shared.blockColorKey
import com.luxalpa.structuredhighlights.shared.DefinitionBlockDescriptor
import com.luxalpa.structuredhighlights.shared.LanguageSupport
import org.intellij.lang.annotations.Language
import org.jetbrains.plugins.scss.SCSSElementTypes
import org.jetbrains.plugins.scss.psi.SCSSFile
import org.jetbrains.plugins.scss.psi.SassScssFunctionDeclaration

class ScssLangSupp : LanguageSupport {
    override val blockTypes: List<BlockType> = ScssBlockType.entries
    override val displayName: String = "SCSS"
    override val languageId: String = "SCSS"
    override val previewText: String = PREVIEW_TEXT

    override fun collectDescriptors(file: PsiFile): List<DefinitionBlockDescriptor>? {
        val file = file as? SCSSFile ?: return null

        val collector = BlockCollector()

        file.stylesheet.rulesetList.children.forEach { child ->
            when {
                child.node?.elementType == SCSSElementTypes.SCSS_MIXIN_DECLARATION -> {
                    val nameIdentifier = (child as? PsiNameIdentifierOwner)?.nameIdentifier
                    collector.collectBlock(ScssBlockType.MIXIN, child, nameIdentifier)
                }

                child is SassScssFunctionDeclaration -> {
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
    override val baseColor: BaseColor,
) : BlockType {
    MIXIN("Mixin", BaseColor.CLASS),
    FUNCTION("Function", BaseColor.FUNCTION);

    override val key: String = "LUX_SH_SCSS_BG_$name"
    override val colorKey: ColorKey = blockColorKey(key, baseColor)
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