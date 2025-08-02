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

const val COLOR_ALPHA: Double = 0.035
const val HEADING_ALPHA: Double = 0.1
const val SUBHEADING_ALPHA: Double = 0.06

enum class BlockType {
    ENUM,
    STRUCT,
    TRAIT,
    IMPL,
    FUNCTION,
    MODULE;

    fun defaultColor(): Color = when (this) {
        ENUM -> Color(-1083409)
        STRUCT -> Color(-15329590)
        TRAIT -> Color(-16521928)
        IMPL -> Color(-6724070)
        FUNCTION -> Color(-6743526)
        MODULE -> Color(-10066330)
    }

    fun defaultHighlightColor(): Color = when (this) {
        ENUM -> Color(-593418)
        STRUCT -> Color(-921096)
        TRAIT -> Color(-1181971)
        IMPL -> Color(-922131)
        FUNCTION -> Color(-462094)
        MODULE -> Color(-1381654)
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

        val settings = editor.getUserData(LUX_PREVIEW_SETTINGS) ?: LxApplicationSettings.instance

        for (descriptor in descriptors) {
            val highlighter = if (descriptor.mode == Mode.EXACT_RANGE) {
                markupModel.addRangeHighlighter(
                    descriptor.startOffset,
                    descriptor.endOffset,
                    HighlighterLayer.GUARDED_BLOCKS + 1,
                    TextAttributes(
                        null,
                        settings.getHighlightColor(descriptor.blockType),
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

                highlighter.customRenderer = LxHighlightingRenderer(
                    descriptor.blockType, descriptor.kind, settings
                )

                highlighter
            }

            newHighlighters.add(highlighter)
        }

        // Store the new highlighters so they can be disposed later
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

        val baseColor = settings.getColor(blockType)
        val alpha = settings.getOpacity(kind)

        val colorWithAlpha = Color(
            baseColor.red / 255f, baseColor.green / 255f, baseColor.blue / 255f,
            alpha.toFloat()
        )

        g.color = colorWithAlpha
        g.fillRect(0, startPosY, width, height)
    }
}