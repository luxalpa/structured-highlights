package com.luxalpa.structuredhighlights.languages

import com.intellij.lang.ASTNode
import com.intellij.lang.ecmascript6.psi.ES6Class
import com.intellij.lang.javascript.JSRecursiveNodeVisitor
import com.intellij.lang.javascript.psi.JSFile
import com.intellij.lang.javascript.psi.JSFunction
import com.intellij.lang.javascript.psi.JSFunctionExpression
import com.intellij.lang.javascript.psi.JSVarStatement
import com.intellij.lang.javascript.psi.JSVariable
import com.intellij.lang.javascript.psi.types.JSRecursiveTypeVisitor
import com.intellij.openapi.editor.colors.ColorKey
import com.intellij.psi.PsiFile
import com.luxalpa.structuredhighlights.BlockType
import com.luxalpa.structuredhighlights.DefinitionBlockDescriptor
import com.luxalpa.structuredhighlights.Descriptor
import com.luxalpa.structuredhighlights.Kind
import com.luxalpa.structuredhighlights.LanguageSupport
import com.luxalpa.structuredhighlights.debug
import java.awt.Color

class JavaScript : LanguageSupport {
    override val displayName: String = "JavaScript"
    override val languageId: String = "JavaScript"
    override val previewText: String = PREVIEW_TEXT
    override val blockTypes: List<BlockType> = JsBlockType.entries

    override fun collectDescriptors(file: PsiFile): List<DefinitionBlockDescriptor>? {
        val file = file as? JSFile ?: return null

        val collector = BlockCollector()

        file.statements.forEach { statement ->
            when (statement) {
                is JSFunction -> {
                    val descriptors = buildList {
                        add(Descriptor(Kind.Block, statement))
                        statement.nameIdentifier?.let {
                            add(Descriptor(Kind.Header, it))
                            add(Descriptor(Kind.Identifier, it))
                        }
                    }

                    collector.collect(JsBlockType.FUNCTION, descriptors) {}
                }

                is JSVarStatement -> {
                    statement.declarations.forEach { declaration ->
                        if (declaration !is JSVariable) return@forEach
                        (declaration.initializer as? JSFunctionExpression)?.let {
                            val descriptors = buildList {
                                add(Descriptor(Kind.Block, statement))
                                declaration.nameIdentifier?.let {
                                    add(Descriptor(Kind.Header, it))
                                    add(Descriptor(Kind.Identifier, it))
                                }
                            }

                            collector.collect(JsBlockType.FUNCTION, descriptors) {}
                        }
                    }
                }

                is ES6Class -> {
                    val descriptors = buildList {
                        add(Descriptor(Kind.Block, statement))
                        statement.nameIdentifier?.let {
                            add(Descriptor(Kind.Header, it))
                            add(Descriptor(Kind.Identifier, it))
                        }
                    }

                    collector.collect(JsBlockType.CLASS, descriptors) {
                        statement.functions.forEach { f ->
                            val descriptors = buildList {
                                f.nameIdentifier?.let {
                                    add(Descriptor(Kind.Subheader, it))
                                    add(Descriptor(Kind.Identifier, it))
                                }
                            }

                            collector.collect(JsBlockType.FUNCTION, descriptors) {}
                        }
                    }
                }
            }
        }

        return collector.definitions
    }
}

enum class JsBlockType(
    override val label: String,
    override val defaultColor: Color,
) : BlockType {
    FUNCTION("Function", DefaultColor.FUNCTION),
    CLASS("Class", DefaultColor.CLASS);

    override val key: String get() = "LUX_SH_JS_BG_$name"
    override val colorKey: ColorKey = ColorKey.createColorKey(key, defaultColor)
}

private val PREVIEW_TEXT = """
    function myDragon() {
        doSomething();
    }

    const v = () => {
        return false
    }

    class Dragon {

    }
""".trimIndent()