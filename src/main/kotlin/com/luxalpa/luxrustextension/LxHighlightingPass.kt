package com.luxalpa.luxrustextension

import com.intellij.codeHighlighting.Pass
import com.intellij.codeHighlighting.TextEditorHighlightingPass
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactory
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactoryRegistrar
import com.intellij.codeHighlighting.TextEditorHighlightingPassRegistrar
import com.intellij.codeHighlighting.TextEditorHighlightingPassRegistrar.Anchor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.CustomHighlighterRenderer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
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

const val COLOR_STRENGTH: Int = 180
const val COLOR_MIDDLE: Int = 110
const val COLOR_BASE: Int = 10
const val COLOR_ALPHA: Int = 10

enum class BlockType {
    ENUM,
    STRUCT,
    TRAIT,
    IMPL,
    MODULE,
    FUNCTION;

    fun toColor(): Color = when (this) {
        ENUM -> Color(COLOR_STRENGTH, COLOR_BASE, COLOR_STRENGTH, COLOR_ALPHA)
        STRUCT -> Color(COLOR_BASE, COLOR_BASE, COLOR_STRENGTH, COLOR_ALPHA)
        TRAIT -> Color(COLOR_BASE, COLOR_STRENGTH, COLOR_BASE, COLOR_ALPHA)
        IMPL -> Color(COLOR_STRENGTH, COLOR_MIDDLE, COLOR_BASE, COLOR_ALPHA)
        MODULE -> Color(COLOR_STRENGTH, COLOR_STRENGTH, COLOR_STRENGTH, COLOR_ALPHA)
        FUNCTION -> Color(COLOR_STRENGTH, COLOR_BASE, COLOR_BASE, COLOR_ALPHA)
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
    val mode: Mode = Mode.FULL_LINE
)

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

            override fun visitStructItem(o: RsStructItem) {
                val isTopLevel = curBlockType == null
                curBlockType = curBlockType ?: BlockType.STRUCT
                definitions += DefinitionBlockDescriptor(o.startOffset, o.endOffset, curBlockType!!)
                super.visitStructItem(o)
                if (isTopLevel) curBlockType = null
            }

            override fun visitImplItem(o: RsImplItem) {
                val isTopLevel = curBlockType == null
                curBlockType = curBlockType ?: BlockType.IMPL
                definitions += DefinitionBlockDescriptor(o.startOffset, o.endOffset, curBlockType!!)
                super.visitImplItem(o)
                if (isTopLevel) curBlockType = null
            }

            override fun visitFunction(o: RsFunction) {
                val isTopLevel = curBlockType == null
                curBlockType = curBlockType ?: BlockType.FUNCTION
                definitions += DefinitionBlockDescriptor(o.startOffset, o.endOffset, curBlockType!!)
                super.visitFunction(o)
                if (isTopLevel) curBlockType = null
            }

            override fun visitModItem(o: RsModItem) {
                // Modules will still be colored like their parents, but freestanding modules will not pass on their
                // color to their children.
                val isTopLevel = curBlockType == null
                val blockType = curBlockType ?: BlockType.MODULE
                definitions += DefinitionBlockDescriptor(o.startOffset, o.endOffset, blockType)
                super.visitModItem(o)
                if (isTopLevel) curBlockType = null
            }

            override fun visitTraitItem(o: RsTraitItem) {
                val isTopLevel = curBlockType == null
                curBlockType = curBlockType ?: BlockType.TRAIT
                definitions += DefinitionBlockDescriptor(o.startOffset, o.endOffset, curBlockType!!)
                super.visitTraitItem(o)
                if (isTopLevel) curBlockType = null
            }

            override fun visitEnumItem(o: RsEnumItem) {
                val isTopLevel = curBlockType == null
                curBlockType = curBlockType ?: BlockType.ENUM
                definitions += DefinitionBlockDescriptor(o.startOffset, o.endOffset, curBlockType!!)
                super.visitEnumItem(o)
                if (isTopLevel) curBlockType = null
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
            val highlighter = markupModel.addRangeHighlighter(
                descriptor.startOffset,
                descriptor.endOffset,
                -1,
                null,
                HighlighterTargetArea.LINES_IN_RANGE
            )

            highlighter.customRenderer = LxHighlightingRenderer(descriptor.blockType)

            newHighlighters.add(highlighter)
        }

        // Store the new highlighters so they can be disposed later
        editor.putUserData(LX_HIGHLIGHTERS, newHighlighters)
    }

}

class LxHighlightingRenderer(val blockType: BlockType) : CustomHighlighterRenderer {
    override fun paint(editor: Editor, highlighter: RangeHighlighter, g: java.awt.Graphics) {
        val startLine = editor.offsetToVisualLine(highlighter.startOffset, true)
        val endLine = editor.offsetToVisualLine(highlighter.endOffset, false)

        val startPosY = editor.visualLineToYRange(startLine)[0]
        val endPosY = editor.visualLineToYRange(endLine)[1]
        val height = endPosY - startPosY
        val width = editor.contentComponent.width

        val customColor = JBColor(blockType.toColor(), blockType.toColor()) // light/dark

        g.color = customColor
        g.fillRect(0, startPosY, width, height)
    }
}