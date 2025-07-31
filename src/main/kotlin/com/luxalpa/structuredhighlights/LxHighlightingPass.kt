package com.luxalpa.structuredhighlights

import com.intellij.codeHighlighting.*
import com.intellij.codeHighlighting.TextEditorHighlightingPassRegistrar.Anchor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.*
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.rust.lang.core.psi.RsFile
import java.awt.Color
import java.awt.Font

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

data class DefinitionBlockDescriptor(
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
                    TextAttributes(
                        Color(0.0f, 0.0f, 0.0f),
                        Color(0.96f, 0.95f, 0.93f),
                        null,
                        null,
                        Font.PLAIN
                    ),

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

                val colorOverride = editor.getUserData(LUX_PREVIEW_COLOR)

                highlighter.customRenderer = LxHighlightingRenderer(
                    descriptor.blockType, descriptor.alpha, colorOverride
                )

                highlighter
            }

            newHighlighters.add(highlighter)
        }

        // Store the new highlighters so they can be disposed later
        editor.putUserData(LX_HIGHLIGHTERS, newHighlighters)
    }
}

class LxHighlightingRenderer(val blockType: BlockType, val alpha: Float, val colorOverride: Color?) :
    CustomHighlighterRenderer {
    override fun paint(editor: Editor, highlighter: RangeHighlighter, g: java.awt.Graphics) {
        val startLine = editor.offsetToVisualLine(highlighter.startOffset, true)
        val endLine = editor.offsetToVisualLine(highlighter.endOffset, false)

        val startPosY = editor.visualLineToYRange(startLine)[0]
        val endPosY = editor.visualLineToYRange(endLine)[1]
        val height = endPosY - startPosY
        val width = editor.contentComponent.width

        val baseColor = colorOverride ?: blockType.toColor()
        val customColor = Color(baseColor.red / 255f, baseColor.green / 255f, baseColor.blue / 255f, alpha)

        g.color = customColor
        g.fillRect(0, startPosY, width, height)
    }
}