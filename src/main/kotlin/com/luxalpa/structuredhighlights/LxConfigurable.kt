package com.luxalpa.structuredhighlights

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectLocator
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.ui.EditorTextField
import com.intellij.util.LocalTimeCounter
import com.intellij.util.ui.FormBuilder
import org.rust.lang.RsFileType
import javax.swing.JComponent

class LxConfigurable : Configurable {
    var mySettingsComponent: AppSettingsComponent? = null

    override fun getDisplayName(): @NlsContexts.ConfigurableName String? = "Lux Configurable"

    override fun createComponent(): JComponent? {
        mySettingsComponent = AppSettingsComponent()
        return mySettingsComponent!!.getPanel()
    }

    override fun getPreferredFocusedComponent(): JComponent? {
        return mySettingsComponent?.getPreferredFocusedComponent()
    }

    override fun isModified(): Boolean {
        return false
    }

    override fun apply() {

    }

    override fun disposeUIResources() {
        mySettingsComponent = null
    }
}


class MyLanguageTextField(val myProject: Project, text: String) :
    EditorTextField(this.createDocument(text, myProject), myProject, RsFileType) {
    //    LanguageTextField(Language.findLanguageByID("Rust")!!, project, text, false) {
    companion object {
        fun createDocument(value: String, project: Project): Document {
            val factory = PsiFileFactory.getInstance(project)
            val stamp = LocalTimeCounter.currentTime()

            val psiFile = ReadAction.compute<PsiFile, RuntimeException> {
                factory.createFileFromText("preview.rs", RsFileType, value, stamp, true, false)
            }
            // No need to guess project in getDocument - we already know it
            val document: Document?
            ProjectLocator.withPreferredProject(psiFile.virtualFile, project).use { ignored ->
                document = ReadAction.compute<Document?, java.lang.RuntimeException?>(ThrowableComputable {
                    PsiDocumentManager.getInstance(project).getDocument(psiFile)
                })
            }
            checkNotNull(document)
            return document!!
        }
    }

    override fun createEditor(): EditorEx {
        val editor = super.createEditor()
        editor.isEmbeddedIntoDialogWrapper = true
//        editor.highlighter = HighlighterFactory.createHighlighter(project, RsFileType)
        editor.setHorizontalScrollbarVisible(true)
        editor.setVerticalScrollbarVisible(true)
        editor.settings.isLineNumbersShown = true
        editor.settings.isAutoCodeFoldingEnabled = true
        editor.isOneLineMode = false
        return editor
    }
}

class AppSettingsComponent {
    val myMainPanel: JComponent
    val textField: MyLanguageTextField

    init {
        val openProjects = ProjectManager.getInstance().openProjects
        val project = if (openProjects.isNotEmpty()) openProjects[0] else ProjectManager.getInstance().defaultProject

        textField = MyLanguageTextField(project, getPreviewText())
        textField.font = EditorFontType.PLAIN.globalFont

        myMainPanel = FormBuilder.createFormBuilder()
            .addComponentFillVertically(textField, 0)
//            .addComponentFillVertically(JPanel(), 0)
            .panel
    }

    fun getPanel(): JComponent = myMainPanel
    fun getPreferredFocusedComponent(): JComponent = textField

    fun getPreviewText(): String = """
            use glam::Vec3;
            use itertools::Itertools;
            use serde::{Deserialize, Serialize};
            use std::collections::HashMap;
            use std::fmt::Display;
            use std::iter;
            
            #[derive(Debug, Serialize, Deserialize)]
            #[serde(rename_all = "snake_case")]
            pub enum RawAttributeData {
                Float(Vec<f32>),
                Int(Vec<i32>),
                String(Vec<String>),
            }
            
            impl RawAttributeData {
                pub fn len(&self) -> usize {
                    match self {
                        RawAttributeData::Float(v) => v.len(),
                        RawAttributeData::Int(v) => v.len(),
                        RawAttributeData::String(v) => v.len(),
                    }
                }
            
                pub fn kind(&self) -> AttributeType {
                    match self {
                        RawAttributeData::Float(_) => AttributeType::Float,
                        RawAttributeData::Int(_) => AttributeType::Int,
                        RawAttributeData::String(_) => AttributeType::String,
                    }
                }
            }
            
            #[derive(Debug, Serialize, Deserialize)]
            pub struct RawAttribute {
                pub len: usize,
                pub data: RawAttributeData,
            }
        """.trimIndent()

//    fun getTextFieldText(): String = textField.text
//    fun setTextFieldText(newText: String) {
//        textField.text = newText
//    }
}
