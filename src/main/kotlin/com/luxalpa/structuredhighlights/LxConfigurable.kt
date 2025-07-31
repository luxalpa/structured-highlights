package com.luxalpa.structuredhighlights

import com.intellij.lang.Language
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.NlsContexts
import com.intellij.ui.ColorPanel
import com.intellij.ui.JBColor
import com.intellij.ui.LanguageTextField
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Color
import javax.swing.JComponent
import javax.swing.JPanel

class LxConfigurable : Configurable, Configurable.NoScroll, Configurable.NoMargin {
    var mySettingsComponent: AppSettingsComponent? = null

    override fun getDisplayName(): @NlsContexts.ConfigurableName String? = "Structured Highlights"

    override fun createComponent(): JComponent? {
        mySettingsComponent = AppSettingsComponent()
        return mySettingsComponent!!.getPanel()
    }

    override fun getPreferredFocusedComponent(): JComponent? {
        return null
//        return mySettingsComponent?.getPreferredFocusedComponent()
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

val LUX_PREVIEW_COLOR: Key<Color> = Key.create("LUX_PREVIEW_COLOR")

class AppSettingsComponent {
    val myMainPanel: JPanel
    val textField: LxLanguageTextField
    val colorSelect: ColorPanel

    init {
        val openProjects = ProjectManager.getInstance().openProjects
        val project = if (openProjects.isNotEmpty()) openProjects[0] else ProjectManager.getInstance().defaultProject

        textField = LxLanguageTextField(project, getPreviewText())
        colorSelect = ColorPanel()

        colorSelect.addActionListener { event ->
            val color = colorSelect.selectedColor
            textField.editor?.putUserData(LUX_PREVIEW_COLOR, color)
        }

        myMainPanel = JPanel(BorderLayout())

        val leftPanel = panel {
            row("Structs:") { cell(colorSelect) }
            row { label("Label 2") }
            row { label("Label 3") }
            row { label("Label 3") }
            row { label("Label 3") }
            row { label("Label 3") }
            row { label("Label 3") }
            row { label("Label 3") }
            row { label("Label 3") }
            row { label("Label 3") }
            row { label("Label 3") }
            row { label("Label 3") }
            row { label("Label 3") }
            row { label("Label 3") }
            row { label("Label 3") }
            row { label("Label 3") }
            row { label("Label 3") }
            row { label("Label 3") }
            row { label("Label 3") }
            row { label("Label 3") }
            row { label("Label 3") }
            row { label("Label 3") }
            row { label("Label 3") }
            row { label("Label 3") }
            row { label("Label 3") }
            row { label("Label 3") }
            row { label("Label 3") }
            row { label("Label 3") }
            row { label("Label 3") }
        }

        leftPanel.border = JBUI.Borders.empty(10)

        val scrollPanel = JBScrollPane(leftPanel)
        scrollPanel.border = JBUI.Borders.empty()
        textField.editor?.setBorder(JBUI.Borders.empty())

        myMainPanel.border = JBUI.Borders.customLine(JBColor.border(), 1, 0, 0, 0)
        myMainPanel.add(scrollPanel, BorderLayout.WEST)
        myMainPanel.add(textField, BorderLayout.CENTER)
    }

    fun getPanel(): JComponent = myMainPanel

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
}

class LxLanguageTextField(project: Project, text: String) :
    LanguageTextField(Language.findLanguageByID("Rust")!!, project, text, false) {

    init {
        this.font = EditorFontType.PLAIN.globalFont
    }

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
        editor.setBorder(JBUI.Borders.customLine(JBColor.border(), 0, 1, 0, 1))
        return editor
    }
}