package com.luxalpa.luxrustextension

import com.intellij.codeHighlighting.Pass
import com.intellij.codeHighlighting.TextEditorHighlightingPass
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactory
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactoryRegistrar
import com.intellij.codeHighlighting.TextEditorHighlightingPassRegistrar
import com.intellij.codeHighlighting.TextEditorHighlightingPassRegistrar.Anchor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.CustomHighlighterRenderer
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.endOffset
import com.intellij.psi.util.startOffset
import com.intellij.ui.JBColor
import org.rust.lang.core.psi.RsEnumItem
import org.rust.lang.core.psi.RsFile
import org.rust.lang.core.psi.RsFunction
import org.rust.lang.core.psi.RsImplItem
import org.rust.lang.core.psi.RsModItem
import org.rust.lang.core.psi.RsRecursiveVisitor
import org.rust.lang.core.psi.RsStructItem
import org.rust.lang.core.psi.RsTraitItem
import java.awt.Color
import java.awt.Font

class LxHighlightingPassFactory : TextEditorHighlightingPassFactoryRegistrar, TextEditorHighlightingPassFactory,
    DumbAware {
    override fun registerHighlightingPassFactory(
        registrar: TextEditorHighlightingPassRegistrar,
        project: Project
    ) {
        registrar.registerTextEditorHighlightingPass(
            this,
            Anchor.FIRST,
            Pass.UPDATE_FOLDING,  // makes sense only for `Anchor.AFTER`
            false,
            false
        )
    }

    override fun createHighlightingPass(
        file: PsiFile,
        editor: Editor
    ): TextEditorHighlightingPass = LxHighlightingPass(file, editor)
}

const val COLOR_STRENGTH: Float = 0.6f
const val COLOR_MIDDLE: Float = 0.4f
const val COLOR_BASE: Float = 0.1f
const val COLOR_ALPHA: Float = 0.035f
const val HEADING_ALPHA: Float = 0.1f
const val SUBHEADING_ALPHA: Float = 0.06f

enum class BlockType {
    ENUM,
    STRUCT,
    TRAIT,
    IMPL,
    FUNCTION,
    MODULE;

    fun toColor(): Color = when (this) {
        ENUM -> Color(COLOR_STRENGTH, COLOR_BASE, COLOR_STRENGTH)
        STRUCT -> Color(COLOR_BASE, COLOR_BASE, COLOR_STRENGTH)
        TRAIT -> Color(COLOR_BASE, COLOR_STRENGTH, COLOR_BASE)
        IMPL -> Color(0.6f, 0.4f, 0.1f)
        FUNCTION -> Color(COLOR_STRENGTH, COLOR_BASE, COLOR_BASE)
        MODULE -> Color(COLOR_MIDDLE, COLOR_MIDDLE, COLOR_MIDDLE)
    }

    fun toHeadingBackgroundColor(): Color = when (this) {
        ENUM -> Color(COLOR_STRENGTH, COLOR_BASE, COLOR_STRENGTH)
        STRUCT -> Color(COLOR_BASE, COLOR_BASE, COLOR_STRENGTH)
        TRAIT -> Color(COLOR_BASE, COLOR_STRENGTH, COLOR_BASE)
        IMPL -> Color(0.95f, 0.92f, 0.9f)
        FUNCTION -> Color(0.6f, 0.2f, 0.2f)
        MODULE -> Color(COLOR_MIDDLE, COLOR_MIDDLE, COLOR_MIDDLE)
    }
}

enum class Mode {
    FULL_LINE,
    EXACT_RANGE;
}

class DefinitionBlockDescriptor(
    val startOffset: Int,
    val endOffset: Int,
    val blockType: BlockType,
    val actualType: BlockType,
    val alpha: Float = COLOR_ALPHA,
    val mode: Mode = Mode.FULL_LINE
)

enum class Kind {
    Block, Header, Subheader, Identifier
}

data class Descriptor(val kind: Kind, val element: PsiElement)

private val LX_DESCRIPTORS: Key<List<DefinitionBlockDescriptor>> = Key.create("LX_DESCRIPTORS")
private val LX_HIGHLIGHTERS: Key<List<RangeHighlighter>> = Key.create("LX_HIGHLIGHTERS")

class LxHighlightingPass(
    private val file: PsiFile,
    private val editor: Editor
) : TextEditorHighlightingPass(file.project, editor.document, false), DumbAware {
    override fun doCollectInformation(progress: ProgressIndicator) {
        val file = file as? RsFile ?: return

        val definitions = mutableListOf<DefinitionBlockDescriptor>()

        file.accept(object : RsRecursiveVisitor() {
            var curBlockType: BlockType? = null

            private fun handleBlockType(
                defaultType: BlockType,
                descriptors: List<Descriptor>,
                useForChildren: Boolean = true,
                visit: () -> Unit
            ) {
                val isTopLevel = curBlockType == null
                val newBlockType = curBlockType ?: defaultType
                if (useForChildren) {
                    curBlockType = newBlockType
                }

                for (descriptor in descriptors) {
                    val alpha = when (descriptor.kind) {
                        Kind.Block -> COLOR_ALPHA
                        Kind.Header, Kind.Identifier -> HEADING_ALPHA
                        Kind.Subheader -> SUBHEADING_ALPHA
                    }

                    val mode = when (descriptor.kind) {
                        Kind.Block, Kind.Header, Kind.Subheader -> Mode.FULL_LINE
                        Kind.Identifier -> Mode.EXACT_RANGE
                    }

                    definitions += DefinitionBlockDescriptor(
                        descriptor.element.startOffset,
                        descriptor.element.endOffset,
                        newBlockType,
                        defaultType,
                        alpha,
                        mode
                    )
                }

                visit()
                if (isTopLevel) curBlockType = null
            }

            override fun visitStructItem(o: RsStructItem) {
                val descriptors = buildList {
                    add(Descriptor(Kind.Block, o))
                    o.identifier?.let {
                        add(Descriptor(Kind.Header, it))
                        add(Descriptor(Kind.Identifier, it))
                    }
                }

                handleBlockType(BlockType.STRUCT, descriptors) {
                    super.visitStructItem(o)
                }
            }

            override fun visitImplItem(o: RsImplItem) {
                val descriptors = buildList {
                    add(Descriptor(Kind.Block, o))
                    o.typeReference?.let {
                        add(Descriptor(Kind.Header, it))
                        add(Descriptor(Kind.Identifier, it))
                    }
                    o.traitRef?.let {
                        add(Descriptor(Kind.Identifier, it))
                    }
                }

                handleBlockType(BlockType.IMPL, descriptors) {
                    super.visitImplItem(o)
                }
            }

            override fun visitFunction(o: RsFunction) {
                val isTopLevel = curBlockType == null
                val descriptors = buildList {
                    add(Descriptor(Kind.Block, o))
                    o.identifier.let {
                        add(Descriptor(if (isTopLevel) Kind.Header else Kind.Subheader, it))
                        add(Descriptor(Kind.Identifier, it))
                    }
                }

                handleBlockType(BlockType.FUNCTION, descriptors) {
                    super.visitFunction(o)
                }
            }

            override fun visitModItem(o: RsModItem) {
                // Modules will still be colored like their parents, but freestanding modules will not pass on their
                // color to their children.
                val descriptors = buildList {
                    add(Descriptor(Kind.Block, o))
                    o.identifier.let {
                        add(Descriptor(Kind.Header, it))
                        add(Descriptor(Kind.Identifier, it))
                    }
                }

                handleBlockType(BlockType.MODULE, descriptors, false) {
                    super.visitModItem(o)
                }
            }

            override fun visitTraitItem(o: RsTraitItem) {
                val descriptors = buildList {
                    add(Descriptor(Kind.Block, o))
                    o.identifier?.let {
                        add(Descriptor(Kind.Header, it))
                        add(Descriptor(Kind.Identifier, it))
                    }
                }

                handleBlockType(BlockType.TRAIT, descriptors) {
                    super.visitTraitItem(o)
                }
            }

            override fun visitEnumItem(o: RsEnumItem) {
                val descriptors = buildList {
                    add(Descriptor(Kind.Block, o))
                    o.identifier?.let {
                        add(Descriptor(Kind.Header, it))
                        add(Descriptor(Kind.Identifier, it))
                    }
                }

                handleBlockType(BlockType.ENUM, descriptors) {
                    super.visitEnumItem(o)
                }
            }
        })

        editor.putUserData(LX_DESCRIPTORS, definitions)
    }

    override fun doApplyInformationToEditor() {
        val descriptors = editor.getUserData(LX_DESCRIPTORS) ?: return

        // Remove old highlighters.
        editor.getUserData(LX_HIGHLIGHTERS)?.forEach { it.dispose() }
        editor.putUserData(LX_HIGHLIGHTERS, null)

        val markupModel = editor.markupModel
        val newHighlighters = mutableListOf<RangeHighlighter>()

        for (descriptor in descriptors) {
            val highlighter = if (descriptor.mode == Mode.EXACT_RANGE) {
                markupModel.addRangeHighlighter(
                    descriptor.startOffset,
                    descriptor.endOffset,
                    HighlighterLayer.GUARDED_BLOCKS + 1,
//                    if (descriptor.actualType == BlockType.FUNCTION) {
                    TextAttributes(
//                            Color(0.4f, 0.3f, 0.1f),
                        Color(0.0f, 0.0f, 0.0f),
//                            null,
                        Color(0.96f, 0.95f, 0.93f),
                        null,
                        null,
//                            Color(0.4f, 0.3f, 0.1f),
//                            EffectType.BOLD_LINE_UNDERSCORE,
                        Font.PLAIN
                    ),
//                    } else {
//                        TextAttributes(
//                            null,
////                        Color(1.0f, 1.0f, 1.0f),
//                            null,
////                        descriptor.blockType.toHeadingBackgroundColor(),
//                            //Color(0.6f, 0.6f, 1.0f),
//                            null,
//                            null,
////                        Color(0.5f, 0.5f, 0.9f),
////                        EffectType.SLIGHTLY_WIDER_BOX,
//                            Font.BOLD
//                        )
//                    },

                    HighlighterTargetArea.EXACT_RANGE
                )
            } else {
                val highlighter = markupModel.addRangeHighlighter(
                    descriptor.startOffset,
                    descriptor.endOffset,
                    -1,
                    null,
                    HighlighterTargetArea.LINES_IN_RANGE
                )

                highlighter.customRenderer = LxHighlightingRenderer(descriptor.blockType, descriptor.alpha)

                highlighter
            }

            newHighlighters.add(highlighter)
        }

        // Store the new highlighters so they can be disposed later
        editor.putUserData(LX_HIGHLIGHTERS, newHighlighters)
    }

}

class LxHighlightingRenderer(val blockType: BlockType, val alpha: Float) : CustomHighlighterRenderer {
    override fun paint(editor: Editor, highlighter: RangeHighlighter, g: java.awt.Graphics) {
        val startLine = editor.offsetToVisualLine(highlighter.startOffset, true)
        val endLine = editor.offsetToVisualLine(highlighter.endOffset, false)

        val startPosY = editor.visualLineToYRange(startLine)[0]
        val endPosY = editor.visualLineToYRange(endLine)[1]
        val height = endPosY - startPosY
        val width = editor.contentComponent.width

        val baseColor = blockType.toColor()
        val customColor = Color(baseColor.red / 255f, baseColor.green / 255f, baseColor.blue / 255f, alpha)

        g.color = customColor
        g.fillRect(0, startPosY, width, height)
    }
}