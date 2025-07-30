package com.luxalpa.structuredhighlights

import com.intellij.application.options.colors.*
import com.intellij.lang.Language
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.colors.EditorSchemeAttributeDescriptor
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.NlsContexts
import com.intellij.ui.LanguageTextField
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

const val LUX_RUST_EXTENSION_COLOR_SETTINGS_ID = "Structured Highlights"

class LxColorsPageFactory : ColorAndFontPanelFactory {
    override fun createPanel(options: ColorAndFontOptions): NewColorAndFontPanel {
        val schemesPanel = SchemesPanel(options)
        val optionsPanel = OptionsPanelImpl(
            options, schemesPanel, "Structured Highlights", LxColorDescriptionPanel()
        )
        val previewPanel = LxPreviewPanel()

        schemesPanel.addListener(object : ColorAndFontSettingsListener.Abstract() {
            override fun schemeChanged(source: Any) {
                previewPanel.setColorScheme(options.selectedScheme)
                optionsPanel.updateOptionsList()
            }
        })

        return NewColorAndFontPanel(
            schemesPanel, optionsPanel, previewPanel, "Structured Highlights", null, null
        )
    }

    override fun getPanelDisplayName(): @NlsContexts.ConfigurableName String = "Structured Highlights"
}

class LxColorDescriptionPanel : OptionsPanelImpl.ColorDescriptionPanel {
    override fun getPanel(): JComponent {
        return panel {
            row {
                label("Rust Language Extension")
            }
        }
    }

    override fun resetDefault() {
    }

    override fun reset(description: EditorSchemeAttributeDescriptor) {
    }

    override fun apply(
        descriptor: EditorSchemeAttributeDescriptor,
        scheme: EditorColorsScheme?
    ) {
    }

    override fun addListener(listener: OptionsPanelImpl.ColorDescriptionPanel.Listener) {
    }

}

class LxLanguageTextField(project: Project, text: String) :
    LanguageTextField(Language.findLanguageByID("Rust")!!, project, text, false) {
    override fun createEditor(): EditorEx {
        val editor = super.createEditor()
        editor.setHorizontalScrollbarVisible(true)
        editor.setVerticalScrollbarVisible(true)
        editor.settings.isLineNumbersShown = true
        editor.settings.isAutoCodeFoldingEnabled = true
        editor.settings.isFoldingOutlineShown = true
        editor.settings.isIndentGuidesShown = true
        editor.settings.isLineMarkerAreaShown = true
        editor.settings.isCaretRowShown = true
        editor.isOneLineMode = false
        return editor
    }
}

class LxPreviewPanel : PreviewPanel {
    val myTextField: LxLanguageTextField

    init {
        val openProjects = ProjectManager.getInstance().openProjects
        val project = if (openProjects.isNotEmpty()) openProjects[0] else ProjectManager.getInstance().defaultProject

        myTextField = LxLanguageTextField(project, getPreviewText())
        myTextField.font = EditorFontType.PLAIN.globalFont
    }

    override fun blinkSelectedHighlightType(selected: Any?) {}

    override fun disposeUIResources() {
    }

    override fun getPanel(): JComponent? = myTextField.component

    fun getPreviewText(): String = """
            trait Terrible {
                fn breathe_fire(&self);
                fn devour(&self, num_people: usize);
            }

            #[derive(Clone, Debug)]
            struct Dragon {
                pub name: String,
                pub age: f32
            }

            impl Dragon {
                pub fn roar(&self) {
                    println!("Roar!!!");
                }
            }

            impl Terrible for Dragon {
                fn breathe_fire(&self) {
                    println!("Breathing fire!");
                    self.roar();
                }
                fn devour(&self, num_people: usize) {
                    println!("Devouring {} snacks", num_people);
                    self.roar();
                }
            }

            enum Weapon {
                Tail,
                Claws { num_talons: usize },
                Wings,
                Teeth(usize),
                Fire,
            }

            #[cfg(test)]
            mod tests {
                use super::*;

                fn test_dragon() {
                    let dragon = Dragon {
                        name: "Smaug".to_string(),
                        age: 7000.0,
                    };

                    dragon.breathe_fire();
                }
            }
        """.trimIndent()

    override fun updateView() {
//        val text = getPreviewText()

//        myEditor.document.setText(text)
    }

    override fun addListener(listener: ColorAndFontSettingsListener) {

    }

    fun setColorScheme(highlighterSettings: EditorColorsScheme) {
    }
}