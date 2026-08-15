package com.luxalpa.structuredhighlights

import com.intellij.codeHighlighting.*
import com.intellij.codeHighlighting.TextEditorHighlightingPassRegistrar.Anchor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.*
import com.intellij.openapi.editor.markup.CustomHighlighterOrder
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.rust.lang.core.psi.RsFile
import java.awt.Color

enum class BlockType {
    ENUM,
    STRUCT,
    TRAIT,
    IMPL,
    FUNCTION,
    MODULE;

    fun defaultColor(): Color = when (this) {
        ENUM -> Color(-2490113)
        STRUCT -> Color(-16756225)
        TRAIT -> Color(-16521928)
        IMPL -> Color(-20992)
        FUNCTION -> Color(-842752)
        MODULE -> Color(-10066330)
    }

    fun label(): String {
        return when (this) {
            ENUM -> "Enum"
            STRUCT -> "Struct"
            TRAIT -> "Trait"
            IMPL -> "Impl"
            FUNCTION -> "Function"
            MODULE -> "Module"
        }
    }
}

enum class Mode {
    FULL_LINE,
    EXACT_RANGE;
}

data class DefinitionBlockDescriptor(
    val startOffset: Int,
    val endOffset: Int,
    val blockType: BlockType,
    val actualType: BlockType,
    val kind: Kind = Kind.Block,
    val mode: Mode = Mode.FULL_LINE
)

enum class Kind {
    Block, Header, Subheader, Identifier
}

data class Descriptor(val kind: Kind, val element: PsiElement)

private val LX_DESCRIPTORS: Key<List<DefinitionBlockDescriptor>> = Key.create("LX_DESCRIPTORS")
private val LX_HIGHLIGHTERS: Key<MutableList<RangeHighlighter>> = Key.create("LX_HIGHLIGHTERS")

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

class LxHighlightingPass(
    private val file: PsiFile,
    private val editor: Editor
) : TextEditorHighlightingPass(file.project, editor.document, false), DumbAware {
    override fun doCollectInformation(progress: ProgressIndicator) {
        val file = file as? RsFile ?: return
        val visitor = RustVisitor()

        file.accept(visitor)

        editor.putUserData(LX_DESCRIPTORS, visitor.definitions)
    }

    override fun doApplyInformationToEditor() {
        val descriptors = editor.getUserData(LX_DESCRIPTORS) ?: return

        // Remove old highlighters.
        val oldHighlighters = editor.getUserData(LX_HIGHLIGHTERS)
        oldHighlighters?.forEach { it.dispose() }
        oldHighlighters?.clear()

        editor.putUserData(LX_HIGHLIGHTERS, null)

        val markupModel = editor.markupModel
        val newHighlighters = mutableListOf<RangeHighlighter>()

        val settings = editor.getUserData(LUX_PREVIEW_SETTINGS) ?: LxApplicationSettings.instance

        for (descriptor in descriptors) {
            val highlighter = if (descriptor.mode == Mode.EXACT_RANGE) {
                markupModel.addRangeHighlighter(
                    descriptor.startOffset,
                    descriptor.endOffset,
                    HighlighterLayer.GUARDED_BLOCKS + 1,
                    null,
                    HighlighterTargetArea.EXACT_RANGE
                ).apply {
                    customRenderer = LxExactRangeHighlightingRenderer(
                        descriptor.blockType,
                        descriptor.kind,
                        settings
                    )
                }
            } else {
                val highlighter = markupModel.addRangeHighlighter(
                    descriptor.startOffset,
                    descriptor.endOffset,
                    -1,
                    null,
                    HighlighterTargetArea.LINES_IN_RANGE
                )

                highlighter.customRenderer = LxHighlightingRenderer(
                    descriptor.blockType, descriptor.kind, settings
                )

                highlighter
            }

            newHighlighters.add(highlighter)
        }

        // Store the new highlighters so they can be disposed of later
        editor.putUserData(LX_HIGHLIGHTERS, newHighlighters)
    }
}

class LxHighlightingRenderer(val blockType: BlockType, val kind: Kind, val settings: AppSettings) :
    CustomHighlighterRenderer {
    override fun paint(editor: Editor, highlighter: RangeHighlighter, g: java.awt.Graphics) {
        val startLine = editor.offsetToVisualLine(highlighter.startOffset, true)
        val endLine = editor.offsetToVisualLine(highlighter.endOffset, false)

        val startPosY = editor.visualLineToYRange(startLine)[0]
        val endPosY = editor.visualLineToYRange(endLine)[1]
        val height = endPosY - startPosY
        val width = editor.contentComponent.width

        val baseColor = editor.colorsScheme.getColor(COLOR_KEYS.getValue(blockType)) ?: blockType.defaultColor()
        val alpha = settings.getOpacity(kind).toFloat()

        val backgroundColor = editor.colorsScheme.defaultBackground

        g.color = Color(
            baseColor.red / 255f * alpha + backgroundColor.red / 255f * (1f - alpha),
            baseColor.green / 255f * alpha + backgroundColor.green / 255f * (1f - alpha),
            baseColor.blue / 255f * alpha + backgroundColor.blue / 255f * (1f - alpha),
        )

        g.fillRect(0, startPosY, width, height)
    }

    override fun getOrder(): CustomHighlighterOrder {
        return CustomHighlighterOrder.BEFORE_BACKGROUND
    }
}

class LxExactRangeHighlightingRenderer(
    private val blockType: BlockType,
    private val kind: Kind,
    private val settings: AppSettings
) : CustomHighlighterRenderer {
    override fun paint(editor: Editor, highlighter: RangeHighlighter, g: java.awt.Graphics) {
        val start = editor.offsetToXY(highlighter.startOffset)
        val end = editor.offsetToXY(highlighter.endOffset)
        val lineHeight = editor.lineHeight

        val baseColor =
            editor.colorsScheme.getColor(COLOR_KEYS.getValue(blockType))
                ?: blockType.defaultColor()
        val backgroundColor = editor.colorsScheme.defaultBackground
        val alpha = settings.getOpacity(kind).toFloat()

        g.color = Color(
            baseColor.red / 255f * alpha + backgroundColor.red / 255f * (1f - alpha),
            baseColor.green / 255f * alpha + backgroundColor.green / 255f * (1f - alpha),
            baseColor.blue / 255f * alpha + backgroundColor.blue / 255f * (1f - alpha),
        )

        if (start.y == end.y) {
            g.fillRect(start.x, start.y, end.x - start.x, lineHeight)
        }
    }

    override fun getOrder(): CustomHighlighterOrder =
        CustomHighlighterOrder.BEFORE_BACKGROUND
}