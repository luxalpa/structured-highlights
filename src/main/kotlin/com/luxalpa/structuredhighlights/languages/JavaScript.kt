package com.luxalpa.structuredhighlights.languages

import com.intellij.lang.ecmascript6.psi.ES6Class
import com.intellij.lang.javascript.psi.JSFile
import com.intellij.lang.javascript.psi.JSFunction
import com.intellij.lang.javascript.psi.JSFunctionExpression
import com.intellij.lang.javascript.psi.JSVarStatement
import com.intellij.lang.javascript.psi.JSVariable
import com.intellij.lang.javascript.psi.ecma6.TypeScriptClass
import com.intellij.lang.javascript.psi.ecma6.TypeScriptInterface
import com.intellij.lang.javascript.psi.ecma6.TypeScriptTypeAlias
import com.intellij.openapi.editor.colors.ColorKey
import com.intellij.psi.PsiFile
import com.luxalpa.structuredhighlights.BlockType
import com.luxalpa.structuredhighlights.DefinitionBlockDescriptor
import com.luxalpa.structuredhighlights.Descriptor
import com.luxalpa.structuredhighlights.Kind
import com.luxalpa.structuredhighlights.LanguageSupport
import org.intellij.lang.annotations.Language
import org.rust.openapiext.document
import java.awt.Color

class JavaScript : LanguageSupport {
    override val displayName: String = "JavaScript / TypeScript"
    override val languageId: String = "TypeScript"
    override val previewText: String = PREVIEW_TEXT
    override val blockTypes: List<BlockType> = TsBlockType.entries

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

                    collector.collect(TsBlockType.FUNCTION, descriptors)
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

                            collector.collect(TsBlockType.FUNCTION, descriptors)
                        }
                    }
                }

                is ES6Class, is TypeScriptClass -> {
                    val descriptors = buildList {
                        add(Descriptor(Kind.Block, statement))
                        statement.nameIdentifier?.let {
                            add(Descriptor(Kind.Header, it))
                            add(Descriptor(Kind.Identifier, it))
                        }
                    }

                    collector.collect(TsBlockType.CLASS, descriptors) {
                        statement.functions.forEach { f ->
                            val descriptors = buildList {
                                f.nameIdentifier?.let {
                                    add(Descriptor(Kind.Subheader, it))
                                    add(Descriptor(Kind.Identifier, it))
                                }
                            }

                            collector.collect(TsBlockType.FUNCTION, descriptors)
                        }
                    }
                }

                is TypeScriptInterface -> {
                    val descriptors = buildList {
                        add(Descriptor(Kind.Block, statement))
                        statement.nameIdentifier?.let {
                            add(Descriptor(Kind.Header, it))
                            add(Descriptor(Kind.Identifier, it))
                        }
                    }

                    collector.collect(TsBlockType.INTERFACE, descriptors)
                }

                is TypeScriptTypeAlias -> {
                    statement.typeDeclaration?.let { typeDecl ->
                        val document = typeDecl.containingFile.document ?: return@let
                        val range = typeDecl.textRange
                        val startLine = document.getLineNumber(range.startOffset)
                        val endLine = document.getLineNumber(range.endOffset.coerceAtMost(document.textLength))
                        val lineSpan = endLine - startLine + 1
                        if (lineSpan <= 2)
                            return@let
                        val descriptors = buildList {
                            add(Descriptor(Kind.Block, statement))
                            statement.nameIdentifier?.let {
                                add(Descriptor(Kind.Header, it))
                                add(Descriptor(Kind.Identifier, it))
                            }
                        }

                        collector.collect(TsBlockType.TYPEDECL, descriptors)
                    }
                }
            }
        }

        return collector.definitions
    }
}

enum class TsBlockType(
    override val label: String,
    override val defaultColor: Color,
) : BlockType {
    FUNCTION("Function", DefaultColor.FUNCTION),
    CLASS("Class", DefaultColor.CLASS),
    INTERFACE("Interface", DefaultColor.INTERFACE),
    TYPEDECL("Type alias", DefaultColor.STRUCT);

    override val key: String get() = "LUX_SH_JS_BG_$name"
    override val colorKey: ColorKey = ColorKey.createColorKey(key, defaultColor)
}

@Language("TypeScript")
private val PREVIEW_TEXT = """
    function spawnDragon() {
        let dragon = new Dragon();
    }

    const spawnWyvern = () => {
        let wyvern: Wyvern = new Dragon();
        return wyvern;
    }
    
    type Wyvern = {
        name: string
    }
    
    class Dragon implements Wyvern {
        name: string;
    }
    
    interface Hungry {
        hungerLevel: number;
    }
""".trimIndent()